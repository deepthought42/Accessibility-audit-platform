package com.looksee.auditService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.PageAuditProgressMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

/**
 * Per-service tests for audit-service after the migration to
 * {@link com.looksee.messaging.web.PubSubAuditController}. Envelope
 * validation, base64/JSON edge cases, idempotency claim/release, and metrics
 * emission are owned by the base class and covered in
 * {@code PubSubAuditControllerTest}; this file exercises only the
 * messageType dispatch and broadcast logic in
 * {@link AuditController#processInTransaction(com.looksee.models.message.Message)}.
 */
class AuditControllerTest {

    private AuditController auditController;
    private AuditRecordService auditRecordService;
    private AccountService accountService;
    private DomainService domainService;
    private PageStateService pageStateService;
    private MessageBroadcaster messageBroadcaster;

    @BeforeEach
    void setUp() {
        auditController = new AuditController();
        auditRecordService = mock(AuditRecordService.class);
        accountService = mock(AccountService.class);
        domainService = mock(DomainService.class);
        pageStateService = mock(PageStateService.class);
        messageBroadcaster = mock(MessageBroadcaster.class);

        ReflectionTestUtils.setField(auditController, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(auditController, "account_service", accountService);
        ReflectionTestUtils.setField(auditController, "domain_service", domainService);
        ReflectionTestUtils.setField(auditController, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(auditController, "messageBroadcaster", messageBroadcaster);
        ReflectionTestUtils.setField(auditController, "self", auditController);
    }

    // ========== AuditProgressUpdate dispatch ==========

    @Test
    void auditProgressUpdate_RecordNotPresent_noBroadcast() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        when(auditRecordService.findById(100L)).thenReturn(Optional.empty());

        auditController.processInTransaction(msg);

        verify(messageBroadcaster, org.mockito.Mockito.never()).sendAuditUpdate(any(), any());
    }

    @Test
    void auditProgressUpdate_DomainAuditPresent_NotComplete_broadcastsBoth() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        setupPageAuditMocks(100L, false);

        DomainAuditRecord domainAuditRecord = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAuditRecord));
        setupDomainAuditMocks(200L, domainAuditRecord, false);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("100"), any(AuditUpdateDto.class));
        verify(messageBroadcaster).sendAuditUpdate(eq("200"), any(AuditUpdateDto.class));
    }

    @Test
    void auditProgressUpdate_DomainAuditComplete_emailsUser() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

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

        auditController.processInTransaction(msg);

        verify(domainService).findByAuditRecord(200L);
    }

    @Test
    void auditProgressUpdate_AllAuditsPresent_completeStatus_emailsUserForPage() throws Exception {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        // Full audit set across all categories so calculateProgress() reaches 1.0,
        // execution_status becomes COMPLETE, and the dispatch enters the email branch
        // that calls getPageStateForAuditRecord(...).
        Set<Audit> audits = createFullAuditSet();
        when(auditRecordService.getAllAudits(100L)).thenReturn(audits);

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(100);

        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");
        when(accountService.findById(1L)).thenReturn(Optional.of(account));

        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com/page");
        when(auditRecordService.getPageStateForAuditRecord(100L)).thenReturn(pageState);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("100"), any(AuditUpdateDto.class));
        verify(auditRecordService).getPageStateForAuditRecord(100L);
    }

    // ========== PageAuditProgressMessage dispatch ==========

    @Test
    void pageAuditProgress_NotComplete_DomainAuditAbsent_broadcastsPage() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(10L);
        when(auditRecordService.findPage(100L)).thenReturn(page);
        when(pageStateService.getElementStateCount(10L)).thenReturn(100);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("100"), any(AuditUpdateDto.class));
    }

    @Test
    void pageAuditProgress_DomainAuditPresent_broadcastsDomain() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        PageAuditRecord pageAuditRecord = createPageAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(pageAuditRecord));

        DomainAuditRecord domainAudit = createDomainAuditRecord(200L);
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.of(domainAudit));

        setupDomainAuditMocks(200L, domainAudit, false);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("200"), any(AuditUpdateDto.class));
    }

    @Test
    void pageAuditProgress_RecordNotPageAuditRecord_returnsSilently() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        DomainAuditRecord domainRecord = createDomainAuditRecord(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(domainRecord));

        auditController.processInTransaction(msg);

        verify(messageBroadcaster, org.mockito.Mockito.never()).sendAuditUpdate(any(), any());
    }

    @Test
    void pageAuditProgress_RecordNotFound_returnsSilently() throws Exception {
        PageAuditProgressMessage msg = new PageAuditProgressMessage();
        msg.setPageAuditId(100L);
        msg.setAccountId(1L);

        when(auditRecordService.findById(100L)).thenReturn(Optional.empty());

        auditController.processInTransaction(msg);

        verify(messageBroadcaster, org.mockito.Mockito.never()).sendAuditUpdate(any(), any());
    }

    // ========== JourneyCandidate / Verified / Discarded dispatch ==========

    @Test
    void journeyCandidate_broadcastsDomainUpdate() throws Exception {
        JourneyCandidateMessage msg = new JourneyCandidateMessage();
        msg.setAuditRecordId(300L);

        DomainAuditRecord domainAudit = createDomainAuditRecord(300L);
        setupDomainAuditMocks(300L, domainAudit, false);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("300"), any(AuditUpdateDto.class));
    }

    @Test
    void verifiedJourney_broadcastsDomainUpdate() throws Exception {
        VerifiedJourneyMessage msg = new VerifiedJourneyMessage();
        msg.setAuditRecordId(400L);

        DomainAuditRecord domainAudit = createDomainAuditRecord(400L);
        setupDomainAuditMocks(400L, domainAudit, false);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("400"), any(AuditUpdateDto.class));
    }

    @Test
    void discardedJourney_broadcastsDomainUpdate() throws Exception {
        DiscardedJourneyMessage msg = new DiscardedJourneyMessage();
        msg.setAuditRecordId(500L);

        DomainAuditRecord domainAudit = createDomainAuditRecord(500L);
        setupDomainAuditMocks(500L, domainAudit, false);

        auditController.processInTransaction(msg);

        verify(messageBroadcaster).sendAuditUpdate(eq("500"), any(AuditUpdateDto.class));
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
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditName.TEXT_BACKGROUND_CONTRAST));
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditName.NON_TEXT_BACKGROUND_CONTRAST));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.ALT_TEXT));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.READING_COMPLEXITY));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.PARAGRAPHING));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.IMAGE_COPYRIGHT));
        audits.add(createAudit(AuditCategory.CONTENT, AuditName.IMAGE_POLICY));
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
