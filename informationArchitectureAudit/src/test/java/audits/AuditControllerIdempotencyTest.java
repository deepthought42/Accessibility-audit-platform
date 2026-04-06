package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.audit.informationArchitecture.AuditController;
import com.looksee.audit.informationArchitecture.audits.AudioControlAudit;
import com.looksee.audit.informationArchitecture.audits.FormStructureAudit;
import com.looksee.audit.informationArchitecture.audits.HeaderStructureAudit;
import com.looksee.audit.informationArchitecture.audits.IdentifyPurposeAudit;
import com.looksee.audit.informationArchitecture.audits.InputPurposeAudit;
import com.looksee.audit.informationArchitecture.audits.LinksAudit;
import com.looksee.audit.informationArchitecture.audits.MetadataAudit;
import com.looksee.audit.informationArchitecture.audits.OrientationAudit;
import com.looksee.audit.informationArchitecture.audits.PageLanguageAudit;
import com.looksee.audit.informationArchitecture.audits.ReflowAudit;
import com.looksee.audit.informationArchitecture.audits.SecurityAudit;
import com.looksee.audit.informationArchitecture.audits.TableStructureAudit;
import com.looksee.audit.informationArchitecture.audits.TextSpacingAudit;
import com.looksee.audit.informationArchitecture.audits.TitleAndHeaderAudit;
import com.looksee.audit.informationArchitecture.audits.UseOfColorAudit;
import com.looksee.audit.informationArchitecture.audits.VisualPresentationAudit;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private IdempotencyService idempotencyService;
    private AuditRecordService auditRecordService;
    private PageStateService pageStateService;
    private HeaderStructureAudit headerStructureAudit;
    private TableStructureAudit tableStructureAudit;
    private FormStructureAudit formStructureAudit;
    private OrientationAudit orientationAudit;
    private InputPurposeAudit inputPurposeAudit;
    private IdentifyPurposeAudit identifyPurposeAudit;
    private UseOfColorAudit useOfColorAudit;
    private ReflowAudit reflowAudit;
    private LinksAudit linksAudit;
    private AudioControlAudit audioControlAudit;
    private VisualPresentationAudit visualPresentationAudit;
    private PageLanguageAudit pageLanguageAudit;
    private MetadataAudit metadataAudit;
    private TitleAndHeaderAudit titleAndHeaderAudit;
    private TextSpacingAudit textSpacingAudit;
    private SecurityAudit securityAudit;
    private PubSubAuditUpdatePublisherImpl auditUpdateTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        auditRecordService = mock(AuditRecordService.class);
        pageStateService = mock(PageStateService.class);
        headerStructureAudit = mock(HeaderStructureAudit.class);
        tableStructureAudit = mock(TableStructureAudit.class);
        formStructureAudit = mock(FormStructureAudit.class);
        orientationAudit = mock(OrientationAudit.class);
        inputPurposeAudit = mock(InputPurposeAudit.class);
        identifyPurposeAudit = mock(IdentifyPurposeAudit.class);
        useOfColorAudit = mock(UseOfColorAudit.class);
        reflowAudit = mock(ReflowAudit.class);
        linksAudit = mock(LinksAudit.class);
        audioControlAudit = mock(AudioControlAudit.class);
        visualPresentationAudit = mock(VisualPresentationAudit.class);
        pageLanguageAudit = mock(PageLanguageAudit.class);
        metadataAudit = mock(MetadataAudit.class);
        titleAndHeaderAudit = mock(TitleAndHeaderAudit.class);
        textSpacingAudit = mock(TextSpacingAudit.class);
        securityAudit = mock(SecurityAudit.class);
        auditUpdateTopic = mock(PubSubAuditUpdatePublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "header_structure_auditor", headerStructureAudit);
        ReflectionTestUtils.setField(controller, "table_structure_auditor", tableStructureAudit);
        ReflectionTestUtils.setField(controller, "form_structure_auditor", formStructureAudit);
        ReflectionTestUtils.setField(controller, "orientationAudit", orientationAudit);
        ReflectionTestUtils.setField(controller, "inputPurposeAudit", inputPurposeAudit);
        ReflectionTestUtils.setField(controller, "identifyPurposeAudit", identifyPurposeAudit);
        ReflectionTestUtils.setField(controller, "useOfColorAudit", useOfColorAudit);
        ReflectionTestUtils.setField(controller, "reflowAudit", reflowAudit);
        ReflectionTestUtils.setField(controller, "links_auditor", linksAudit);
        ReflectionTestUtils.setField(controller, "audioControlAudit", audioControlAudit);
        ReflectionTestUtils.setField(controller, "visualPresentationAudit", visualPresentationAudit);
        ReflectionTestUtils.setField(controller, "pageLanguageAudit", pageLanguageAudit);
        ReflectionTestUtils.setField(controller, "metadata_auditor", metadataAudit);
        ReflectionTestUtils.setField(controller, "title_and_header_auditor", titleAndHeaderAudit);
        ReflectionTestUtils.setField(controller, "textSpacingAudit", textSpacingAudit);
        ReflectionTestUtils.setField(controller, "security_auditor", securityAudit);
        ReflectionTestUtils.setField(controller, "audit_update_topic", auditUpdateTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() throws Exception {
        String validPayload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "information-architecture-audit")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(auditUpdateTopic, never()).publish(anyString());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Invalid payload tests ---

    @Test
    void shouldReturnBadRequestForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullMessage() throws Exception {
        Body body = new Body();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullData() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-1", "2024-01-01T00:00:00Z", "placeholder");
        try {
            java.lang.reflect.Field dataField = Body.Message.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(msg, null);
        } catch (Exception e) {
            fail("Failed to set data field to null via reflection");
        }
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForEmptyData() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-3", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "information-architecture-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-4", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-4", "information-architecture-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    // --- Successful processing tests ---

    @Test
    void shouldCallMarkProcessedOnSuccess() throws Exception {
        String payload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "information-architecture-audit")).thenReturn(false);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(50L);
        when(pageStateService.getPageStateForAuditRecord(100L)).thenReturn(page);
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());

        Audit mockAudit = mock(Audit.class);
        when(mockAudit.getId()).thenReturn(1L);
        when(headerStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(tableStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(formStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(orientationAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(inputPurposeAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(identifyPurposeAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(useOfColorAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(reflowAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(linksAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(audioControlAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(visualPresentationAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(pageLanguageAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(metadataAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(titleAndHeaderAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(textSpacingAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(securityAudit.execute(any(), any(), any())).thenReturn(mockAudit);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully"));
        verify(auditUpdateTopic).publish(anyString());
        verify(idempotencyService).markProcessed("msg-5", "information-architecture-audit");
    }

    @Test
    void shouldReturnNotFoundForMissingAuditRecord() throws Exception {
        String payload = "{\"accountId\":1,\"pageAuditId\":999}";
        Body body = createValidBody("msg-6", payload);
        when(idempotencyService.isAlreadyProcessed("msg-6", "information-architecture-audit")).thenReturn(false);
        when(auditRecordService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-7", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-7", "information-architecture-audit")).thenReturn(false);

        controller.receiveMessage(body);

        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Helper ---

    private Body createValidBody(String messageId, String jsonPayload) {
        String encoded = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message(messageId, "2024-01-01T00:00:00Z", encoded);
        body.setMessage(msg);
        return body;
    }
}
