package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.mapper.Body;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.PageAuditProgressMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

class AuditControllerTest {

    private AuditController auditController;
    private AuditRecordService auditRecordService;
    private AccountService accountService;
    private DomainService domainService;
    private PageStateService pageStateService;
    private MessageBroadcaster messageBroadcaster;
    private IdempotencyService idempotencyService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        auditController = new AuditController();
        auditRecordService = mock(AuditRecordService.class);
        accountService = mock(AccountService.class);
        domainService = mock(DomainService.class);
        pageStateService = mock(PageStateService.class);
        messageBroadcaster = mock(MessageBroadcaster.class);
        idempotencyService = mock(IdempotencyService.class);

        setField(auditController, "audit_record_service", auditRecordService);
        setField(auditController, "account_service", accountService);
        setField(auditController, "domain_service", domainService);
        setField(auditController, "page_state_service", pageStateService);
        setField(auditController, "messageBroadcaster", messageBroadcaster);
        setField(auditController, "idempotencyService", idempotencyService);

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Body createBody(String json) {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn(encoded);
        return body;
    }

    // ========== Input Validation Tests ==========

    @Test
    void receiveMessageShouldReturnBadRequestForNullBody() throws Exception {
        ResponseEntity<String> response = auditController.receiveMessage(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForNullMessage() throws Exception {
        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(null);
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForNullData() throws Exception {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn(null);
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForInvalidBase64Payload() throws Exception {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn("not-base64$payload");
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForUnknownMessageType() throws Exception {
        Body body = createBody("{\"unexpected\":\"message\"}");
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Error occurred while updating audit progress"));
    }

    @Test
    void receiveMessageWithEmptyData() throws Exception {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn("");
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== AuditProgressUpdate Tests ==========

    @Test
    void receiveMessageAuditProgressUpdate_RecordNotPresent() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        when(auditRecordService.findById(100L)).thenReturn(Optional.empty());

        // When record is not found, the first try block still returns 200 OK
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_DomainAuditPresent_NotComplete() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);
        msg.setProgress(0.5);
        msg.setMessage("testing");

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, false);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));
        setupDomainAuditMocks(200L, domainAuditRecord, false);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(messageBroadcaster).sendAuditUpdate(eq("100"), any(AuditUpdateDto.class));
        verify(messageBroadcaster).sendAuditUpdate(eq("200"), any(AuditUpdateDto.class));
    }

    @Test
    void receiveMessageAuditProgressUpdate_DomainAuditPresent_Complete() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));
        setupDomainAuditMocks(200L, domainAuditRecord, true);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        Domain domain = mock(Domain.class);
        when(domain.getUrl()).thenReturn("https://example.com");
        when(domainService.findByAuditRecord(200L)).thenReturn(domain);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_DomainAuditPresent_Complete_NoAccount() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));
        setupDomainAuditMocks(200L, domainAuditRecord, true);

        when(accountService.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_NoDomainAudit_NotComplete() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, false);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_NoDomainAudit_Complete() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com/page");
        when(auditRecordService.getPageStateForAuditRecord(100L)).thenReturn(pageState);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_NoDomainAudit_Complete_NullPageState() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        when(auditRecordService.getPageStateForAuditRecord(100L)).thenReturn(null);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessageAuditProgressUpdate_NoDomainAudit_Complete_AllAuditsPresent() throws Exception {
        // Cover lines 143-151: NoDomainAudit + COMPLETE status from buildPageAuditUpdatedDto
        // Need all 11 audits with correct category/name so all progress values >= 1.0
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        // Create audits covering all 11 labels with correct categories
        Set<Audit> audits = createFullAuditSet();
        when(auditRecordService.getAllAudits(100L)).thenReturn(audits);

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(100);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        // Need account for the ifPresent lambda at line 146
        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com/page");
        when(auditRecordService.getPageStateForAuditRecord(100L)).thenReturn(pageState);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== PageAuditProgressMessage Tests ==========
    // Note: AuditProgressUpdate and PageAuditProgressMessage share the same fields,
    // so the first try-catch block (AuditProgressUpdate) always handles the message.
    // To reach the PageAuditProgressMessage handler, we must make the first block throw
    // an exception during processing (not during deserialization).

    @Test
    void receiveMessagePageAuditProgress_NotComplete_DomainAuditPresent() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        // First try block: findById returns present, then buildPageAuditUpdatedDto calls
        // getAllAudits which we make throw to force falling through to second handler
        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L))
            .thenReturn(Optional.of(pageAuditRecord))  // 1st call in AuditProgressUpdate handler
            .thenReturn(Optional.of(pageAuditRecord)); // 2nd call in PageAuditProgressMessage handler

        // Make the first handler's buildPageAuditUpdatedDto throw by having getAllAudits throw
        when(auditRecordService.getAllAudits(100L))
            .thenThrow(new RuntimeException("force first handler to fail"))
            .thenReturn(new HashSet<>()); // return normally for second handler

        // Setup for second handler (PageAuditProgressMessage)
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        // For buildPageAuditUpdatedDto in second handler's else branch
        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(100);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessagePageAuditProgress_Complete_DomainAuditPresent() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L))
            .thenReturn(Optional.of(pageAuditRecord))
            .thenReturn(Optional.of(pageAuditRecord));

        // First handler throws
        when(auditRecordService.getAllAudits(100L))
            .thenThrow(new RuntimeException("force first handler to fail"))
            .thenReturn(new HashSet<>());

        DomainAuditRecord domainAudit = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAudit));
        when(auditRecordService.isDomainAuditComplete(domainAudit)).thenReturn(false);

        setupDomainAuditMocks(200L, domainAudit, false);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessagePageAuditProgress_Complete_DomainAuditPresent_DomainComplete() throws Exception {
        // Cover lines 192-194: PageAuditProgressMessage where isDomainAuditComplete returns true
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L))
            .thenReturn(Optional.of(pageAuditRecord))
            .thenReturn(Optional.of(pageAuditRecord));

        // First handler throws
        when(auditRecordService.getAllAudits(100L))
            .thenThrow(new RuntimeException("force first handler to fail"))
            .thenReturn(new HashSet<>()); // empty audits → isPageAuditComplete returns true (empty labels)

        DomainAuditRecord domainAudit = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAudit));
        when(auditRecordService.isDomainAuditComplete(domainAudit)).thenReturn(true);

        // Account for the ifPresent lambda at lines 192-194
        Account account = mock(Account.class);
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        setupDomainAuditMocks(200L, domainAudit, true);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessagePageAuditProgress_Complete_NoDomainAudit() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L))
            .thenReturn(Optional.of(pageAuditRecord))
            .thenReturn(Optional.of(pageAuditRecord));

        // First handler throws
        when(auditRecordService.getAllAudits(100L))
            .thenThrow(new RuntimeException("force first handler to fail"))
            .thenReturn(new HashSet<>());

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        Account account = mock(Account.class);
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(100);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessagePageAuditProgress_RecordNotFound() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        // First handler: findById returns empty => returns 200 OK (logs "Unknown record type")
        // The PageAuditProgressMessage handler is never reached in this case
        when(auditRecordService.findById(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        // First try block handles it and returns 200 OK even if record not found
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receiveMessagePageAuditProgress_RecordNotPageAuditRecord() throws Exception {
        // Test the case where PageAuditProgressMessage handler gets a non-PageAuditRecord
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        // First handler: record found but buildPageAuditUpdatedDto throws
        DomainAuditRecord domainRecord = createDomainAuditRecord(100L);
        when(auditRecordService.findById(100L))
            .thenReturn(Optional.of(domainRecord))  // first call in AuditProgressUpdate
            .thenReturn(Optional.of(domainRecord)); // second call: returns DomainAuditRecord (not PageAuditRecord)

        // Make first handler throw
        when(auditRecordService.getAllAudits(100L))
            .thenThrow(new RuntimeException("force first handler to fail"));

        // Second handler: findById returns DomainAuditRecord (not PageAuditRecord) => BAD_REQUEST
        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== JourneyCandidateMessage Tests ==========

    @Test
    void receiveMessageJourneyCandidateMessage() throws Exception {
        JourneyCandidateMessage msg = new JourneyCandidateMessage();
        msg.setAuditRecordId(300L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        // This JSON also deserializes as AuditProgressUpdate/PageAuditProgressMessage
        // First two handlers will process but may throw when they can't find the record
        when(auditRecordService.findById(anyLong())).thenReturn(Optional.empty());

        // For buildDomainAuditRecordDTO called by JourneyCandidateMessage handler
        DomainAuditRecord domainAudit = createDomainAuditRecord(300L);
        when(auditRecordService.findById(300L))
            .thenReturn(Optional.empty())   // AuditProgressUpdate handler
            .thenReturn(Optional.empty())   // PageAuditProgressMessage handler (if reached)
            .thenReturn(Optional.of(domainAudit)); // JourneyCandidateMessage handler

        setupDomainAuditMocks(300L, domainAudit, false);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== VerifiedJourneyMessage Tests ==========

    @Test
    void receiveMessageVerifiedJourneyMessage_ReachesHandler() throws Exception {
        // Cover lines 237-241: Force all prior handlers to throw so VerifiedJourneyMessage handler executes
        VerifiedJourneyMessage msg = new VerifiedJourneyMessage();
        msg.setAuditRecordId(400L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        // When deserialized as AuditProgressUpdate/PageAuditProgressMessage, pageAuditId=0
        // Handler 1 & 2: findById(0) returns a record, then getAllAudits(0) throws
        PageAuditRecord dummyRecord = createPageAuditRecord(0L);
        when(auditRecordService.findById(0L))
            .thenReturn(Optional.of(dummyRecord))   // handler 1
            .thenReturn(Optional.of(dummyRecord));   // handler 2

        when(auditRecordService.getAllAudits(0L))
            .thenThrow(new RuntimeException("force handler 1 to fail"))   // handler 1 (buildPageAuditUpdatedDto)
            .thenThrow(new RuntimeException("force handler 2 to fail"));  // handler 2 (line 177)

        // Handler 3 (JourneyCandidateMessage): auditRecordId=400 → buildDomainAuditRecordDTO(400) → findById(400) empty → throws
        // Handler 4 (VerifiedJourneyMessage): auditRecordId=400 → buildDomainAuditRecordDTO(400) → findById(400) returns record
        DomainAuditRecord domainAudit = createDomainAuditRecord(400L);
        when(auditRecordService.findById(400L))
            .thenReturn(Optional.empty())              // handler 3: causes IllegalArgumentException
            .thenReturn(Optional.of(domainAudit));     // handler 4: success

        // Setup domain audit mocks for handler 4's buildDomainAuditRecordDTO
        when(auditRecordService.getAllPageAudits(400L)).thenReturn(new HashSet<>());
        when(auditRecordService.getNumberOfJourneysWithStatus(400L, JourneyStatus.CANDIDATE)).thenReturn(0);
        when(auditRecordService.getNumberOfJourneys(400L)).thenReturn(5);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== DiscardedJourneyMessage Tests ==========

    @Test
    void receiveMessageDiscardedJourneyMessage() throws Exception {
        DiscardedJourneyMessage msg = new DiscardedJourneyMessage();
        msg.setAuditRecordId(500L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        DomainAuditRecord domainAudit = createDomainAuditRecord(500L);
        when(auditRecordService.findById(anyLong())).thenReturn(Optional.empty());
        when(auditRecordService.findById(500L))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(domainAudit));

        setupDomainAuditMocks(500L, domainAudit, false);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== buildPageAuditUpdatedDto Tests (via receiveMessage) ==========

    @Test
    void buildPageAuditUpdatedDto_dataExtractionProgressBelow50() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> emptyAudits = new HashSet<>();
        when(auditRecordService.getAllAudits(100L)).thenReturn(emptyAudits);

        // page null => data extraction = 0.0 => message = "Setting up browser"
        when(auditRecordService.findPage(100L)).thenReturn(null);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void buildPageAuditUpdatedDto_dataExtractionProgressBetween50And60() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> emptyAudits = new HashSet<>();
        when(auditRecordService.getAllAudits(100L)).thenReturn(emptyAudits);

        // page exists with 0 elements => milestone=1, progress=1/2=0.5 => message = "Analyzing elements"
        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(0);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPageDataExtractionProgress_withAudits() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> audits = new HashSet<>();
        Audit audit = new Audit();
        audit.setCategory(AuditCategory.CONTENT);
        audit.setPoints(80);
        audit.setTotalPossiblePoints(100);
        audits.add(audit);
        when(auditRecordService.getAllAudits(100L)).thenReturn(audits);

        when(auditRecordService.findPage(100L)).thenReturn(null);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPageDataExtractionProgress_noPage() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> emptyAudits = new HashSet<>();
        when(auditRecordService.getAllAudits(100L)).thenReturn(emptyAudits);
        when(auditRecordService.findPage(100L)).thenReturn(null);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPageDataExtractionProgress_withPageAndElements() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> emptyAudits = new HashSet<>();
        when(auditRecordService.getAllAudits(100L)).thenReturn(emptyAudits);

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(500);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPageDataExtractionProgress_withPageAndElementsExceedingMax() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        Set<Audit> emptyAudits = new HashSet<>();
        when(auditRecordService.getAllAudits(100L)).thenReturn(emptyAudits);

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(2000);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== buildDomainAuditRecordDTO Tests ==========

    @Test
    void buildDomainAuditRecordDTO_notFound() throws Exception {
        JourneyCandidateMessage msg = new JourneyCandidateMessage();
        msg.setAuditRecordId(999L);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        when(auditRecordService.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void buildDomainAuditRecordDTO_completeStatus() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.DOMAIN);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));

        when(auditRecordService.findById(200L)).thenReturn(Optional.of(domainAuditRecord));
        Set<PageAuditRecord> pageAudits = new HashSet<>();
        PageAuditRecord pa = createPageAuditRecord(101L);
        pageAudits.add(pa);
        when(auditRecordService.getAllPageAudits(200L)).thenReturn(pageAudits);
        when(auditRecordService.getAllAuditsForPageAuditRecord(101L)).thenReturn(new HashSet<>());
        when(auditRecordService.getNumberOfJourneysWithStatus(200L, JourneyStatus.CANDIDATE)).thenReturn(0);
        when(auditRecordService.getNumberOfJourneys(200L)).thenReturn(5);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        Domain domain = mock(Domain.class);
        when(domain.getUrl()).thenReturn("https://example.com");
        when(domainService.findByAuditRecord(200L)).thenReturn(domain);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== getDomainDataExtractionProgress Tests ==========

    @Test
    void getDomainDataExtractionProgress_totalJourneysLessThanOrEqual1() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, true);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));

        when(auditRecordService.findById(200L)).thenReturn(Optional.of(domainAuditRecord));
        when(auditRecordService.getAllPageAudits(200L)).thenReturn(new HashSet<>());
        when(auditRecordService.getNumberOfJourneysWithStatus(200L, JourneyStatus.CANDIDATE)).thenReturn(0);
        when(auditRecordService.getNumberOfJourneys(200L)).thenReturn(1);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        Domain domain = mock(Domain.class);
        when(domain.getUrl()).thenReturn("https://example.com");
        when(domainService.findByAuditRecord(200L)).thenReturn(domain);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDomainDataExtractionProgress_withCandidates() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);
        msg.setCategory(AuditCategory.CONTENT);
        msg.setLevel(AuditLevel.PAGE);

        String json = mapper.writeValueAsString(msg);
        Body body = createBody(json);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, false);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));

        when(auditRecordService.findById(200L)).thenReturn(Optional.of(domainAuditRecord));
        when(auditRecordService.getAllPageAudits(200L)).thenReturn(new HashSet<>());
        when(auditRecordService.getNumberOfJourneysWithStatus(200L, JourneyStatus.CANDIDATE)).thenReturn(3);
        when(auditRecordService.getNumberOfJourneys(200L)).thenReturn(10);

        ResponseEntity<String> response = auditController.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== Helper Methods ==========

    private PageAuditRecord createPageAuditRecord(long id) {
        PageAuditRecord record = new PageAuditRecord();
        record.setId(id);
        record.setAuditLabels(new HashSet<>());
        return record;
    }

    private DomainAuditRecord createDomainAuditRecord(long id) {
        DomainAuditRecord record = new DomainAuditRecord();
        record.setId(id);
        record.setAuditLabels(new HashSet<>());
        return record;
    }

    private Set<Audit> createFullAuditSet() {
        Set<Audit> audits = new HashSet<>();
        // AESTHETICS labels (2): TEXT_BACKGROUND_CONTRAST, NON_TEXT_BACKGROUND_CONTRAST
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditName.TEXT_BACKGROUND_CONTRAST));
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditName.NON_TEXT_BACKGROUND_CONTRAST));
        // CONTENT labels (5): ALT_TEXT, READING_COMPLEXITY, PARAGRAPHING, IMAGE_COPYRIGHT, IMAGE_POLICY
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.ALT_TEXT));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.READING_COMPLEXITY));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.PARAGRAPHING));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.IMAGE_COPYRIGHT));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.IMAGE_POLICY));
        // INFORMATION_ARCHITECTURE labels (4): LINKS, TITLES, ENCRYPTED, METADATA
        audits.add(createAudit(AuditCategory.INFORMATION_ARCHITECTURE, AuditName.LINKS));
        audits.add(createAudit(AuditCategory.INFORMATION_ARCHITECTURE, AuditName.TITLES));
        audits.add(createAudit(AuditCategory.INFORMATION_ARCHITECTURE, AuditName.ENCRYPTED));
        audits.add(createAudit(AuditCategory.INFORMATION_ARCHITECTURE, AuditName.METADATA));
        return audits;
    }

    private Audit createAudit(AuditCategory category, AuditName name) {
        Audit audit = new Audit();
        audit.setCategory(category);
        audit.setName(name);
        audit.setPoints(100);
        audit.setTotalPossiblePoints(100);
        return audit;
    }

    private void setupPageAuditMocks(long pageAuditId, boolean complete) {
        Set<Audit> audits = new HashSet<>();
        if (complete) {
            for (AuditCategory cat : new AuditCategory[]{AuditCategory.AESTHETICS, AuditCategory.CONTENT, AuditCategory.INFORMATION_ARCHITECTURE}) {
                Audit audit = new Audit();
                audit.setCategory(cat);
                audit.setPoints(100);
                audit.setTotalPossiblePoints(100);
                audits.add(audit);
            }
        }
        when(auditRecordService.getAllAudits(pageAuditId)).thenReturn(audits);

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(pageAuditId * 10);
        when(auditRecordService.findPage(pageAuditId)).thenReturn(page);
        when(pageStateService.getElementStateCount(pageAuditId * 10)).thenReturn(100);
    }

    private void setupDomainAuditMocks(long domainAuditId, DomainAuditRecord domainAuditRecord, boolean complete) {
        when(auditRecordService.findById(domainAuditId)).thenReturn(Optional.of(domainAuditRecord));

        Set<PageAuditRecord> pageAudits = new HashSet<>();
        if (complete) {
            PageAuditRecord pa = createPageAuditRecord(domainAuditId + 1);
            pageAudits.add(pa);
            Set<Audit> audits = new HashSet<>();
            for (AuditCategory cat : new AuditCategory[]{AuditCategory.AESTHETICS, AuditCategory.CONTENT, AuditCategory.INFORMATION_ARCHITECTURE}) {
                Audit audit = new Audit();
                audit.setCategory(cat);
                audit.setPoints(100);
                audit.setTotalPossiblePoints(100);
                audits.add(audit);
            }
            when(auditRecordService.getAllAuditsForPageAuditRecord(domainAuditId + 1)).thenReturn(audits);
        }
        when(auditRecordService.getAllPageAudits(domainAuditId)).thenReturn(pageAudits);

        if (complete) {
            when(auditRecordService.getNumberOfJourneysWithStatus(domainAuditId, JourneyStatus.CANDIDATE)).thenReturn(0);
            when(auditRecordService.getNumberOfJourneys(domainAuditId)).thenReturn(5);
        } else {
            when(auditRecordService.getNumberOfJourneysWithStatus(domainAuditId, JourneyStatus.CANDIDATE)).thenReturn(3);
            when(auditRecordService.getNumberOfJourneys(domainAuditId)).thenReturn(10);
        }
    }
}
