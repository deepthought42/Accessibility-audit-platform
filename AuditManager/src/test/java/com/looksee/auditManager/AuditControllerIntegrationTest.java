package com.looksee.auditManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration tests for {@link AuditController} using Spring MockMvc.
 * Tests the HTTP layer with mocked service dependencies and no live
 * Neo4j or PubSub connections.
 */
@WebMvcTest(AuditController.class)
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditRecordService auditRecordService;

    @MockBean
    private PubSubPageAuditPublisherImpl auditRecordTopic;

    @MockBean
    private PageStateService pageStateService;

    @MockBean
    private IdempotencyService idempotencyService;

    private final ObjectMapper mapper = JacksonConfig.mapper();

    /**
     * Builds a valid PubSub push envelope JSON string with the given inner
     * message serialized and Base64-encoded in the data field.
     */
    private String buildValidEnvelope(Object innerMessage) throws Exception {
        String innerJson = mapper.writeValueAsString(innerMessage);
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"test-msg-001\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                base64Data);
    }

    @Test
    @DisplayName("POST / with valid PageBuiltMessage returns 200 and processes the message")
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
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully processed message"));
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
                .andExpect(status().isOk())
                .andExpect(content().string("Duplicate message, already processed"));

        // Verify no actual processing occurred
        verify(auditRecordService, never()).wasPageAlreadyAudited(anyLong(), anyLong());
        verify(auditRecordTopic, never()).publish(anyString());
    }

    @Test
    @DisplayName("POST / with invalid JSON acknowledges to prevent redelivery")
    void postInvalidJson_returns200Acknowledging() throws Exception {
        // Invalid base64 data
        String invalidEnvelope = "{\"message\":{\"messageId\":\"test-bad\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"not-valid-base64!!!\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-manager")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with null message acknowledges gracefully")
    void postNullMessage_returns200() throws Exception {
        String nullMessageEnvelope = "{\"message\":null}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullMessageEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with empty data field acknowledges gracefully")
    void postEmptyData_returns200() throws Exception {
        String emptyDataEnvelope = "{\"message\":{\"messageId\":\"test-empty\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"\"}}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyDataEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / skips page that was already audited")
    void postAlreadyAuditedPage_returns200Skipping() throws Exception {
        PageBuiltMessage innerMsg = new PageBuiltMessage(1L, 100L, 200L);
        String envelopeJson = buildValidEnvelope(innerMsg);

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("audit-manager")))
                .thenReturn(false);
        when(auditRecordService.wasPageAlreadyAudited(200L, 100L)).thenReturn(true);
        when(auditRecordService.findById(200L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully processed message"));

        verify(auditRecordTopic, never()).publish(anyString());
    }
}
