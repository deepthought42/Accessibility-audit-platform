package com.looksee.contentAudit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.contentAudit.models.AppletAltTextAudit;
import com.looksee.contentAudit.models.CanvasAltTextAudit;
import com.looksee.contentAudit.models.IframeAltTextAudit;
import com.looksee.contentAudit.models.ImageAltTextAudit;
import com.looksee.contentAudit.models.ObjectAltTextAudit;
import com.looksee.contentAudit.models.ParagraphingAudit;
import com.looksee.contentAudit.models.ReadabilityAudit;
import com.looksee.contentAudit.models.SVGAltTextAudit;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * Integration tests for the contentAudit {@link AuditController} using Spring
 * MockMvc. Tests the HTTP layer with mocked dependencies.
 */
@WebMvcTest(AuditController.class)
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private AuditRecordService audit_record_service;

    @MockBean
    private PageStateService page_state_service;

    @MockBean
    private ImageAltTextAudit image_alt_text_auditor;

    @MockBean
    private AppletAltTextAudit appletAllAltTextAudit;

    @MockBean
    private CanvasAltTextAudit canvasAltTextAudit;

    @MockBean
    private IframeAltTextAudit iframeAltTextAudit;

    @MockBean
    private ObjectAltTextAudit objectAltTextAudit;

    @MockBean
    private SVGAltTextAudit svgAltTextAudit;

    @MockBean
    private ParagraphingAudit paragraph_auditor;

    @MockBean
    private ReadabilityAudit readability_auditor;

    @MockBean
    private PubSubAuditUpdatePublisherImpl audit_update_topic;

    private final ObjectMapper mapper = JacksonConfig.mapper();

    private String buildValidEnvelope(String messageId, Object innerMessage) throws Exception {
        String innerJson = mapper.writeValueAsString(innerMessage);
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"%s\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                messageId, base64Data);
    }

    @Test
    @DisplayName("POST / with valid PageAuditMessage performs content audit and returns 200")
    void postValidMessage_returns200() throws Exception {
        PageAuditMessage innerMsg = new PageAuditMessage(1L, 42L);
        String envelope = buildValidEnvelope("msg-001", innerMsg);

        when(idempotencyService.isAlreadyProcessed("msg-001", "content-audit"))
                .thenReturn(false);

        AuditRecord mockRecord = mock(PageAuditRecord.class);
        when(mockRecord.getId()).thenReturn(42L);
        when(audit_record_service.findById(42L)).thenReturn(Optional.of(mockRecord));

        PageState mockPage = mock(PageState.class);
        when(mockPage.getId()).thenReturn(100L);
        when(mockPage.getElements()).thenReturn(new java.util.ArrayList<>());
        when(page_state_service.findByAuditRecordId(42L)).thenReturn(mockPage);
        when(page_state_service.getElementStates(100L)).thenReturn(new java.util.ArrayList<>());
        when(audit_record_service.getAllAudits(42L)).thenReturn(new HashSet<>());

        Audit mockAudit = mock(Audit.class);
        when(mockAudit.getId()).thenReturn(1L);
        when(image_alt_text_auditor.execute(any(), any(), any())).thenReturn(mockAudit);
        when(appletAllAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(canvasAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(iframeAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(objectAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(svgAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(readability_auditor.execute(any(), any(), any())).thenReturn(mockAudit);
        when(paragraph_auditor.execute(any(), any(), any())).thenReturn(mockAudit);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully completed content audit"));
    }

    @Test
    @DisplayName("POST / with duplicate message returns 200 without processing")
    void postDuplicateMessage_returns200WithoutProcessing() throws Exception {
        PageAuditMessage innerMsg = new PageAuditMessage(1L, 42L);
        String envelope = buildValidEnvelope("dup-msg", innerMsg);

        when(idempotencyService.isAlreadyProcessed("dup-msg", "content-audit"))
                .thenReturn(true);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Duplicate message, already processed"));

        verify(audit_record_service, never()).findById(anyLong());
    }

    @Test
    @DisplayName("POST / with invalid JSON acknowledges to prevent redelivery")
    void postInvalidJson_returns200Acknowledging() throws Exception {
        String invalidEnvelope = "{\"message\":{\"messageId\":\"bad-msg\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"not-base64!!!\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("content-audit")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnvelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with null payload returns 200 acknowledging")
    void postNullPayload_returns200() throws Exception {
        String nullPayload = "{\"message\":null}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullPayload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with empty data returns 200 acknowledging")
    void postEmptyData_returns200() throws Exception {
        String emptyData = "{\"message\":{\"messageId\":\"empty-data\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("content-audit")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyData))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with missing audit record returns 200 acknowledging")
    void postMissingAuditRecord_returns200() throws Exception {
        PageAuditMessage innerMsg = new PageAuditMessage(1L, 999L);
        String envelope = buildValidEnvelope("missing-record", innerMsg);

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("content-audit")))
                .thenReturn(false);
        when(audit_record_service.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }
}
