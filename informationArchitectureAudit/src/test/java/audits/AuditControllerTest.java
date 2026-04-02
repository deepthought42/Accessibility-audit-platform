package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.audit.informationArchitecture.AuditController;
import com.looksee.audit.informationArchitecture.audits.*;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.services.AuditRecordService;
import com.looksee.services.PageStateService;

public class AuditControllerTest {

    private AuditController controller;
    private AuditRecordService mockAuditRecordService;
    private PageStateService mockPageStateService;
    private PubSubAuditUpdatePublisherImpl mockPubSubPublisher;

    // Audit mocks
    private HeaderStructureAudit mockHeaderStructureAudit;
    private TableStructureAudit mockTableStructureAudit;
    private FormStructureAudit mockFormStructureAudit;
    private OrientationAudit mockOrientationAudit;
    private InputPurposeAudit mockInputPurposeAudit;
    private IdentifyPurposeAudit mockIdentifyPurposeAudit;
    private UseOfColorAudit mockUseOfColorAudit;
    private ReflowAudit mockReflowAudit;
    private LinksAudit mockLinksAudit;
    private AudioControlAudit mockAudioControlAudit;
    private VisualPresentationAudit mockVisualPresentationAudit;
    private PageLanguageAudit mockPageLanguageAudit;
    private MetadataAudit mockMetadataAudit;
    private TitleAndHeaderAudit mockTitleAndHeaderAudit;
    private TextSpacingAudit mockTextSpacingAudit;
    private SecurityAudit mockSecurityAudit;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AuditController();

        mockAuditRecordService = mock(AuditRecordService.class);
        mockPageStateService = mock(PageStateService.class);
        mockPubSubPublisher = mock(PubSubAuditUpdatePublisherImpl.class);

        mockHeaderStructureAudit = mock(HeaderStructureAudit.class);
        mockTableStructureAudit = mock(TableStructureAudit.class);
        mockFormStructureAudit = mock(FormStructureAudit.class);
        mockOrientationAudit = mock(OrientationAudit.class);
        mockInputPurposeAudit = mock(InputPurposeAudit.class);
        mockIdentifyPurposeAudit = mock(IdentifyPurposeAudit.class);
        mockUseOfColorAudit = mock(UseOfColorAudit.class);
        mockReflowAudit = mock(ReflowAudit.class);
        mockLinksAudit = mock(LinksAudit.class);
        mockAudioControlAudit = mock(AudioControlAudit.class);
        mockVisualPresentationAudit = mock(VisualPresentationAudit.class);
        mockPageLanguageAudit = mock(PageLanguageAudit.class);
        mockMetadataAudit = mock(MetadataAudit.class);
        mockTitleAndHeaderAudit = mock(TitleAndHeaderAudit.class);
        mockTextSpacingAudit = mock(TextSpacingAudit.class);
        mockSecurityAudit = mock(SecurityAudit.class);

        // Inject all mocks via reflection
        injectField("audit_record_service", mockAuditRecordService);
        injectField("page_state_service", mockPageStateService);
        injectField("audit_update_topic", mockPubSubPublisher);
        injectField("header_structure_auditor", mockHeaderStructureAudit);
        injectField("table_structure_auditor", mockTableStructureAudit);
        injectField("form_structure_auditor", mockFormStructureAudit);
        injectField("orientationAudit", mockOrientationAudit);
        injectField("inputPurposeAudit", mockInputPurposeAudit);
        injectField("identifyPurposeAudit", mockIdentifyPurposeAudit);
        injectField("useOfColorAudit", mockUseOfColorAudit);
        injectField("reflowAudit", mockReflowAudit);
        injectField("links_auditor", mockLinksAudit);
        injectField("audioControlAudit", mockAudioControlAudit);
        injectField("visualPresentationAudit", mockVisualPresentationAudit);
        injectField("pageLanguageAudit", mockPageLanguageAudit);
        injectField("metadata_auditor", mockMetadataAudit);
        injectField("title_and_header_auditor", mockTitleAndHeaderAudit);
        injectField("textSpacingAudit", mockTextSpacingAudit);
        injectField("security_auditor", mockSecurityAudit);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuditController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private Body createBody(String data) {
        Body body = new Body();
        Body.Message message = body.new Message();
        message.setData(data);
        body.setMessage(message);
        return body;
    }

    private String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    // --- Input validation tests ---

    @Test
    void testReceiveMessage_nullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("body.message is required"));
    }

    @Test
    void testReceiveMessage_nullMessage() throws Exception {
        Body body = new Body();
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testReceiveMessage_nullData() throws Exception {
        Body body = new Body();
        Body.Message message = body.new Message();
        body.setMessage(message);
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("message.data is required"));
    }

    @Test
    void testReceiveMessage_emptyData() throws Exception {
        Body body = createBody("");
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testReceiveMessage_invalidBase64() throws Exception {
        Body body = createBody("!!!not-valid-base64!!!");
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("base64"));
    }

    @Test
    void testReceiveMessage_emptyDecodedData() throws Exception {
        // Base64 encoding of empty string is "", which triggers the data.isEmpty() check
        // returning "message.data is required" rather than the decoded-empty check
        Body body = createBody(encodeBase64(""));
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testReceiveMessage_invalidJson() throws Exception {
        Body body = createBody(encodeBase64("not-json-at-all"));
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("valid PageAuditMessage JSON"));
    }

    @Test
    void testReceiveMessage_auditRecordNotFound() throws Exception {
        String json = "{\"pageAuditId\":999,\"accountId\":1}";
        Body body = createBody(encodeBase64(json));
        when(mockAuditRecordService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testReceiveMessage_successfulAudit() throws Exception {
        String json = "{\"pageAuditId\":1,\"accountId\":100}";
        Body body = createBody(encodeBase64(json));

        AuditRecord mockRecord = mock(AuditRecord.class);
        when(mockRecord.getId()).thenReturn(1L);
        when(mockAuditRecordService.findById(1L)).thenReturn(Optional.of(mockRecord));

        PageState mockPageState = mock(PageState.class);
        when(mockPageStateService.getPageStateForAuditRecord(1L)).thenReturn(mockPageState);
        when(mockAuditRecordService.getAllAudits(1L)).thenReturn(new HashSet<>());

        // Mock all audit executions to return audit objects with IDs
        Audit mockAudit = mock(Audit.class);
        when(mockAudit.getId()).thenReturn(1L);

        when(mockHeaderStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockTableStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockFormStructureAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockOrientationAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockInputPurposeAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockIdentifyPurposeAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockUseOfColorAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockAudioControlAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockVisualPresentationAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockReflowAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockTextSpacingAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockPageLanguageAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockLinksAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockTitleAndHeaderAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockSecurityAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(mockMetadataAudit.execute(any(), any(), any())).thenReturn(mockAudit);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully audited"));
    }

    @Test
    void testReceiveMessage_auditsAlreadyExist() throws Exception {
        String json = "{\"pageAuditId\":1,\"accountId\":100}";
        Body body = createBody(encodeBase64(json));

        AuditRecord mockRecord = mock(AuditRecord.class);
        when(mockRecord.getId()).thenReturn(1L);
        when(mockAuditRecordService.findById(1L)).thenReturn(Optional.of(mockRecord));

        PageState mockPageState = mock(PageState.class);
        when(mockPageStateService.getPageStateForAuditRecord(1L)).thenReturn(mockPageState);

        // Create a set of existing audits for all audit names
        Set<Audit> existingAudits = new HashSet<>();
        for (AuditName name : AuditName.values()) {
            Audit existingAudit = mock(Audit.class);
            when(existingAudit.getName()).thenReturn(name);
            existingAudits.add(existingAudit);
        }
        when(mockAuditRecordService.getAllAudits(1L)).thenReturn(existingAudits);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // No audit should have been executed since all already exist
        verify(mockHeaderStructureAudit, never()).execute(any(), any(), any());
        verify(mockTableStructureAudit, never()).execute(any(), any(), any());
        verify(mockLinksAudit, never()).execute(any(), any(), any());
    }

    // --- auditAlreadyExists tests ---

    @Test
    void testAuditAlreadyExists_true() throws Exception {
        Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
        method.setAccessible(true);

        Set<Audit> audits = new HashSet<>();
        Audit audit = mock(Audit.class);
        when(audit.getName()).thenReturn(AuditName.HEADER_STRUCTURE);
        audits.add(audit);

        boolean result = (boolean) method.invoke(controller, audits, AuditName.HEADER_STRUCTURE);
        assertTrue(result);
    }

    @Test
    void testAuditAlreadyExists_false() throws Exception {
        Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
        method.setAccessible(true);

        Set<Audit> audits = new HashSet<>();
        Audit audit = mock(Audit.class);
        when(audit.getName()).thenReturn(AuditName.HEADER_STRUCTURE);
        audits.add(audit);

        boolean result = (boolean) method.invoke(controller, audits, AuditName.LINKS);
        assertFalse(result);
    }

    @Test
    void testAuditAlreadyExists_emptySet() throws Exception {
        Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
        method.setAccessible(true);

        Set<Audit> audits = new HashSet<>();

        boolean result = (boolean) method.invoke(controller, audits, AuditName.LINKS);
        assertFalse(result);
    }
}
