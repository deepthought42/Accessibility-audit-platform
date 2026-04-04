package com.looksee.pageBuilder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.enums.AuditLevel;
import com.looksee.browsing.enums.BrowserType;
import com.looksee.models.message.AuditStartMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.ElementStateService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;

/**
 * Integration tests for the PageBuilder {@link AuditController} using Spring
 * MockMvc. Tests the HTTP layer with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private AuditRecordService audit_record_service;

    @Mock
    private BrowserService browser_service;

    @Mock
    private JourneyService journey_service;

    @Mock
    private StepService step_service;

    @Mock
    private PageStateService page_state_service;

    @Mock
    private ElementStateService element_state_service;

    @Mock
    private DomainMapService domain_map_service;

    @Mock
    private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;

    @Mock
    private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;

    @Mock
    private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;

    @Mock
    private PubSubPageAuditPublisherImpl audit_record_topic;

    private final ObjectMapper mapper = JacksonConfig.mapper();

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController();
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "audit_record_service", audit_record_service);
        ReflectionTestUtils.setField(controller, "browser_service", browser_service);
        ReflectionTestUtils.setField(controller, "journey_service", journey_service);
        ReflectionTestUtils.setField(controller, "step_service", step_service);
        ReflectionTestUtils.setField(controller, "page_state_service", page_state_service);
        ReflectionTestUtils.setField(controller, "element_state_service", element_state_service);
        ReflectionTestUtils.setField(controller, "domain_map_service", domain_map_service);
        ReflectionTestUtils.setField(controller, "pubSubErrorPublisherImpl", pubSubErrorPublisherImpl);
        ReflectionTestUtils.setField(controller, "pubSubJourneyVerifiedPublisherImpl", pubSubJourneyVerifiedPublisherImpl);
        ReflectionTestUtils.setField(controller, "pubSubPageCreatedPublisherImpl", pubSubPageCreatedPublisherImpl);
        ReflectionTestUtils.setField(controller, "audit_record_topic", audit_record_topic);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String buildValidEnvelope(String messageId, Object innerMessage) throws Exception {
        String innerJson = mapper.writeValueAsString(innerMessage);
        String base64Data = Base64.getEncoder().encodeToString(
                innerJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"%s\",\"data\":\"%s\"}}",
                messageId, base64Data);
    }

    @Test
    @DisplayName("POST / with null body returns 400")
    void postNullBody_returns400() throws Exception {
        String emptyBody = "{\"message\":null}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / with empty data returns 400")
    void postEmptyData_returns400() throws Exception {
        String envelope = "{\"message\":{\"messageId\":\"test-001\",\"data\":\"\"}}";

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / with duplicate message returns 200 without processing")
    void postDuplicateMessage_returns200() throws Exception {
        AuditStartMessage innerMsg = new AuditStartMessage(
                "https://example.com", BrowserType.CHROME, 100L, AuditLevel.PAGE, 1L);
        String envelope = buildValidEnvelope("dup-msg-001", innerMsg);

        when(idempotencyService.isAlreadyProcessed("dup-msg-001", "page-builder"))
                .thenReturn(true);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk())
                .andExpect(content().string("Duplicate message, already processed"));

        verify(browser_service, never()).getConnection(any(), any());
    }

    @Test
    @DisplayName("POST / with invalid Base64 data returns 200 acknowledging")
    void postInvalidBase64_returns200() throws Exception {
        String envelope = "{\"message\":{\"messageId\":\"bad-b64\",\"data\":\"not!!!valid!!!base64\"}}";

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("page-builder")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST / with invalid JSON in decoded data returns 200 acknowledging")
    void postInvalidInnerJson_returns200() throws Exception {
        String invalidJson = "this is not json";
        String base64 = Base64.getEncoder().encodeToString(
                invalidJson.getBytes(StandardCharsets.UTF_8));
        String envelope = String.format(
                "{\"message\":{\"messageId\":\"bad-json\",\"data\":\"%s\"}}", base64);

        when(idempotencyService.isAlreadyProcessed(anyString(), eq("page-builder")))
                .thenReturn(false);

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope))
                .andExpect(status().isOk());
    }
}
