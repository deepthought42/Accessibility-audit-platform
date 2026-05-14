package com.looksee.auditService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

/**
 * Integration tests for the audit-service {@link AuditController} using
 * Spring MockMvc. After the migration to
 * {@link com.looksee.messaging.web.PubSubAuditController}, these tests
 * exercise the full HTTP entry path: envelope parsing, idempotency claim,
 * polymorphic deserialization, and per-handler dispatch.
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerIntegrationTest {

    private static final String SERVICE = "audit-service";

    private MockMvc mockMvc;
    private AuditController controller;
    private ObjectMapper mapper;

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
    private IdempotencyGuard idempotencyService;

    @Mock
    private PubSubMetrics pubSubMetrics;

    @Mock
    private PoisonMessagePublisher poisonPublisher;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        mapper = new Application().auditServiceObjectMapper();

        ReflectionTestUtils.setField(controller, "audit_record_service", audit_record_service);
        ReflectionTestUtils.setField(controller, "account_service", account_service);
        ReflectionTestUtils.setField(controller, "domain_service", domain_service);
        ReflectionTestUtils.setField(controller, "page_state_service", page_state_service);
        ReflectionTestUtils.setField(controller, "messageBroadcaster", messageBroadcaster);
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "objectMapper", mapper);
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
        ReflectionTestUtils.setField(controller, "poisonPublisher", poisonPublisher);
        ReflectionTestUtils.setField(controller, "self", controller);

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
    @DisplayName("POST / with valid AuditProgressUpdate returns 200 ok")
    void postValidAuditProgressUpdate_returns200() throws Exception {
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 1.0, "Content Audit Complete!",
                AuditCategory.CONTENT, AuditLevel.PAGE, 42L);
        String envelope = buildValidEnvelope("msg-progress-001", innerMsg);

        when(idempotencyService.claim("msg-progress-001", SERVICE)).thenReturn(true);

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
                .andExpect(content().string("ok"));

        verify(messageBroadcaster).sendAuditUpdate(anyString(), any());
        verify(pubSubMetrics).recordSuccess(SERVICE, "audit_update");
    }

    @Test
    @DisplayName("Idempotent replay: second delivery short-circuits with Duplicate")
    void postDuplicateMessage_returns200WithoutProcessing() throws Exception {
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 0.5, "In progress",
                AuditCategory.CONTENT, AuditLevel.PAGE, 42L);
        String envelope = buildValidEnvelope("dup-msg-001", innerMsg);

        // Stateful claim: first call wins, every subsequent call sees the row.
        when(idempotencyService.claim("dup-msg-001", SERVICE))
                .thenReturn(true)
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
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Duplicate message, already processed"));

        // Broadcast must happen exactly once across redeliveries.
        verify(messageBroadcaster, times(1)).sendAuditUpdate(anyString(), any());
        verify(pubSubMetrics).recordDuplicate(SERVICE, "audit_update");
    }

    @Test
    @DisplayName("POST / with null message in envelope returns 200 + invalid metric + poison")
    void postNullMessage_returns200WithInvalidMetric() throws Exception {
        String nullPayload = "{\"message\":null}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullPayload))
                .andExpect(status().isOk());

        verify(pubSubMetrics).recordInvalid(SERVICE, "audit_update");
        verifyNoInteractions(messageBroadcaster);
    }

    @Test
    @DisplayName("Malformed Base64 returns 200, records error metric, publishes poison")
    void postInvalidBase64_returns200AndPoisons() throws Exception {
        String envelope = "{\"message\":{\"messageId\":\"bad-b64\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"not-valid!!!\"}}";

        when(idempotencyService.claim("bad-b64", SERVICE)).thenReturn(true);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid payload, acknowledged"));

        verify(pubSubMetrics).recordError(eq(SERVICE), eq("audit_update"), any(IllegalArgumentException.class));
        verify(poisonPublisher).publishPoison(any(), any());
        verifyNoInteractions(messageBroadcaster);
    }

    @Test
    @DisplayName("Transient downstream error returns 500, releases claim, never poisons")
    void postTransientDownstreamFailure_returns500AndReleasesClaim() throws Exception {
        AuditProgressUpdate innerMsg = new AuditProgressUpdate(
                1L, 0.5, "still going",
                AuditCategory.CONTENT, AuditLevel.PAGE, 77L);
        String envelope = buildValidEnvelope("transient-msg", innerMsg);

        when(idempotencyService.claim("transient-msg", SERVICE)).thenReturn(true);
        when(audit_record_service.findById(77L))
                .thenThrow(new TransientDataAccessResourceException("neo4j unavailable"));

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().is5xxServerError());

        verify(idempotencyService).release("transient-msg", SERVICE);
        verify(pubSubMetrics).recordError(eq(SERVICE), eq("audit_update"), any(TransientDataAccessResourceException.class));
        verify(poisonPublisher, never()).publishPoison(any(), any());
        verify(messageBroadcaster, never()).sendAuditUpdate(anyString(), any());
    }

    @Test
    @DisplayName("Envelope with messageType dispatches to PageAuditProgressMessage handler")
    void postPageAuditProgressMessageType_dispatchesCorrectly() throws Exception {
        String innerJson = "{\"messageType\":\"PageAuditProgressMessage\",\"accountId\":1,\"pageAuditId\":55}";
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        String envelope = String.format(
                "{\"message\":{\"messageId\":\"page-progress\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                base64Data);

        when(idempotencyService.claim("page-progress", SERVICE)).thenReturn(true);

        PageAuditRecord mockRecord = mock(PageAuditRecord.class);
        when(mockRecord.getId()).thenReturn(55L);
        when(audit_record_service.findById(55L)).thenReturn(Optional.of(mockRecord));
        when(audit_record_service.getDomainAuditRecordForPageRecord(55L))
                .thenReturn(Optional.empty());
        when(audit_record_service.getAllAudits(55L)).thenReturn(new HashSet<>());
        when(audit_record_service.findPage(55L)).thenReturn(mock(PageState.class));

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(messageBroadcaster).sendAuditUpdate(eq("55"), any());
    }

    @Test
    @DisplayName("Envelope without messageType falls back to AuditProgressUpdate (defaultImpl)")
    void postMissingMessageType_fallsBackToAuditProgressUpdate() throws Exception {
        String innerJson = "{\"accountId\":1,\"pageAuditId\":42,\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        String envelope = String.format(
                "{\"message\":{\"messageId\":\"legacy-msg\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                base64Data);

        when(idempotencyService.claim("legacy-msg", SERVICE)).thenReturn(true);

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
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(messageBroadcaster).sendAuditUpdate(anyString(), any());
    }
}
