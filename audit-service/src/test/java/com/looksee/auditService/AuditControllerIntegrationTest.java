package com.looksee.auditService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

/**
 * Integration tests for the audit-service {@link AuditController} using
 * Spring MockMvc. Tests the HTTP layer with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AuditRecordService audit_record_service;

    @Mock
    private AccountService account_service;

    @Mock
    private DomainService domain_service;

    @Mock
    private PageStateService page_state_service;

    @Mock
    private MessageBroadcaster messageBroadcaster;

    @Mock
    private IdempotencyService idempotencyService;

    private final ObjectMapper mapper = JacksonConfig.mapper();

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController();
        ReflectionTestUtils.setField(controller, "audit_record_service", audit_record_service);
        ReflectionTestUtils.setField(controller, "account_service", account_service);
        ReflectionTestUtils.setField(controller, "domain_service", domain_service);
        ReflectionTestUtils.setField(controller, "page_state_service", page_state_service);
        ReflectionTestUtils.setField(controller, "messageBroadcaster", messageBroadcaster);
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String buildValidEnvelope(String messageId, Object innerMessage) throws Exception {
        String innerJson = mapper.writeValueAsString(innerMessage);
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"%s\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                messageId, base64Data);
    }

    @Test
    @DisplayName("POST / with valid AuditProgressUpdate returns 200")
    void postValidAuditProgressUpdate_returns200() throws Exception {
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 1.0, "Content Audit Complete!",
                AuditCategory.CONTENT, AuditLevel.PAGE, 42L);
        String envelope = buildValidEnvelope("msg-progress-001", innerMsg);

        when(idempotencyService.isAlreadyProcessed("msg-progress-001", "audit-service"))
                .thenReturn(false);

        AuditRecord mockRecord = mock(PageAuditRecord.class);
        when(mockRecord.getId()).thenReturn(42L);
        when(audit_record_service.findById(42L)).thenReturn(Optional.of(mockRecord));
        when(audit_record_service.getAllAudits(42L)).thenReturn(new HashSet<>());
        when(audit_record_service.getDomainAuditRecordForPageRecord(42L))
                .thenReturn(Optional.empty());

        PageState mockPage = mock(PageState.class);
        when(audit_record_service.findPage(42L)).thenReturn(mockPage);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully sent audit update to user"));

        verify(messageBroadcaster).sendAuditUpdate(anyString(), any());
    }

    @Test
    @DisplayName("POST / with duplicate message returns 200 without processing")
    void postDuplicateMessage_returns200WithoutProcessing() throws Exception {
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 0.5, "In progress",
                AuditCategory.CONTENT, AuditLevel.PAGE, 42L);
        String envelope = buildValidEnvelope("dup-msg-001", innerMsg);

        when(idempotencyService.isAlreadyProcessed("dup-msg-001", "audit-service"))
                .thenReturn(true);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Duplicate message, already processed"));

        verify(audit_record_service, never()).findById(anyLong());
        verify(messageBroadcaster, never()).sendAuditUpdate(anyString(), any());
    }

    @Test
    @DisplayName("POST / with null payload returns 400")
    void postNullPayload_returns400() throws Exception {
        String nullPayload = "{\"message\":null}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / with invalid Base64 data returns 200 acknowledging")
    void postInvalidBase64_returns200() throws Exception {
        String envelope = "{\"message\":{\"messageId\":\"bad-b64\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"not-valid!!!\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-service")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with empty data field processes with empty target string")
    void postEmptyData_returns200() throws Exception {
        String envelope = "{\"message\":{\"messageId\":\"empty-data\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-service")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with messageType routing dispatches to correct handler")
    void postWithMessageTypeRouting_dispatchesCorrectly() throws Exception {
        // AuditProgressUpdate has messageType = "AuditProgressUpdate"
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 0.75, "Processing",
                AuditCategory.CONTENT, AuditLevel.PAGE, 55L);
        String envelope = buildValidEnvelope("routed-msg", innerMsg);

        when(idempotencyService.isAlreadyProcessed("routed-msg", "audit-service"))
                .thenReturn(false);

        AuditRecord mockRecord = mock(PageAuditRecord.class);
        when(mockRecord.getId()).thenReturn(55L);
        when(audit_record_service.findById(55L)).thenReturn(Optional.of(mockRecord));
        when(audit_record_service.getAllAudits(55L)).thenReturn(new HashSet<>());
        when(audit_record_service.getDomainAuditRecordForPageRecord(55L))
                .thenReturn(Optional.empty());
        when(audit_record_service.findPage(55L)).thenReturn(mock(PageState.class));

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully sent audit update to user"));
    }

    @Test
    @DisplayName("POST / with unknown messageType falls back to legacy handling")
    void postUnknownMessageType_fallsBackToLegacy() throws Exception {
        // Manually construct a message with an unknown messageType
        String innerJson = "{\"messageType\":\"UnknownType\",\"accountId\":1,\"pageAuditId\":42}";
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        String envelope = String.format(
                "{\"message\":{\"messageId\":\"unknown-type\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                base64Data);

        when(idempotencyService.isAlreadyProcessed("unknown-type", "audit-service"))
                .thenReturn(false);

        AuditRecord mockRecord = mock(PageAuditRecord.class);
        when(mockRecord.getId()).thenReturn(42L);
        when(audit_record_service.findById(42L)).thenReturn(Optional.of(mockRecord));
        when(audit_record_service.getAllAudits(42L)).thenReturn(new HashSet<>());
        when(audit_record_service.getDomainAuditRecordForPageRecord(42L))
                .thenReturn(Optional.empty());
        when(audit_record_service.findPage(42L)).thenReturn(mock(PageState.class));

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }
}
