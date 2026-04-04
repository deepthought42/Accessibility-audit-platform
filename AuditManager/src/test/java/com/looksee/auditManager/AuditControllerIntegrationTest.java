package com.looksee.auditManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * Integration tests for {@link AuditController} using standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerIntegrationTest {

    @Mock private AuditRecordService auditRecordService;
    @Mock private PubSubPageAuditPublisherImpl auditRecordTopic;
    @Mock private PageStateService pageStateService;
    @Mock private IdempotencyService idempotencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController(
                auditRecordService, auditRecordTopic, pageStateService, idempotencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String buildValidEnvelope(Object innerMessage) throws Exception {
        String innerJson = JacksonConfig.mapper().writeValueAsString(innerMessage);
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"test-msg-001\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                base64Data);
    }

    @Test
    @DisplayName("POST / with valid PageBuiltMessage returns 200")
    void postValidMessage_returns200() throws Exception {
        PageBuiltMessage innerMsg = new PageBuiltMessage(1L, 100L, 200L);
        String envelopeJson = buildValidEnvelope(innerMsg);

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-manager")))
                .thenReturn(false);
        when(auditRecordService.wasPageAlreadyAudited(200L, 100L)).thenReturn(false);
        when(pageStateService.isPageLandable(100L)).thenReturn(true);

        PageState mockPageState = mock(PageState.class);
        when(mockPageState.getUrl()).thenReturn("https://example.com");
        when(pageStateService.findById(100L)).thenReturn(Optional.of(mockPageState));

        PageAuditRecord mockAuditRecord = mock(PageAuditRecord.class);
        when(mockAuditRecord.getId()).thenReturn(999L);
        when(auditRecordService.findById(200L)).thenReturn(Optional.empty());
        when(auditRecordService.save(any(AuditRecord.class))).thenReturn(mockAuditRecord);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with duplicate message returns 200 without processing")
    void postDuplicateMessage_returns200WithoutProcessing() throws Exception {
        PageBuiltMessage innerMsg = new PageBuiltMessage(1L, 100L, 200L);
        String envelopeJson = buildValidEnvelope(innerMsg);

        when(idempotencyService.isAlreadyProcessed("test-msg-001", "audit-manager"))
                .thenReturn(true);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson))
                .andExpect(status().isOk());

        verify(auditRecordService, never()).wasPageAlreadyAudited(anyLong(), anyLong());
        verify(auditRecordTopic, never()).publish(anyString());
    }

    @Test
    @DisplayName("POST / with invalid base64 returns 200")
    void postInvalidBase64_returns200() throws Exception {
        String invalidEnvelope = "{\"message\":{\"messageId\":\"test-bad\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"not-valid-base64!!!\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-manager")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with empty body returns 200")
    void postEmptyBody_returns200() throws Exception {
        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with empty data returns 200")
    void postEmptyData_returns200() throws Exception {
        String emptyDataEnvelope = "{\"message\":{\"messageId\":\"test-empty\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"\"}}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyDataEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / skips already audited page")
    void postAlreadyAuditedPage_returns200() throws Exception {
        PageBuiltMessage innerMsg = new PageBuiltMessage(1L, 100L, 200L);
        String envelopeJson = buildValidEnvelope(innerMsg);

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-manager")))
                .thenReturn(false);
        when(auditRecordService.wasPageAlreadyAudited(200L, 100L)).thenReturn(true);
        when(auditRecordService.findById(200L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson))
                .andExpect(status().isOk());

        verify(auditRecordTopic, never()).publish(anyString());
    }
}
