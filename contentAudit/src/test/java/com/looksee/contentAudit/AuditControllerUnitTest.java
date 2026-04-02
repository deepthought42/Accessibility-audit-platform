package com.looksee.contentAudit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.contentAudit.models.AppletAltTextAudit;
import com.looksee.contentAudit.models.CanvasAltTextAudit;
import com.looksee.contentAudit.models.IframeAltTextAudit;
import com.looksee.contentAudit.models.ImageAltTextAudit;
import com.looksee.contentAudit.models.ObjectAltTextAudit;
import com.looksee.contentAudit.models.ParagraphingAudit;
import com.looksee.contentAudit.models.ReadabilityAudit;
import com.looksee.contentAudit.models.SVGAltTextAudit;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.services.AuditRecordService;
import com.looksee.services.PageStateService;

public class AuditControllerUnitTest {

	private AuditController controller;
	private AuditRecordService auditRecordService;
	private PageStateService pageStateService;
	private ImageAltTextAudit imageAltTextAudit;
	private AppletAltTextAudit appletAltTextAudit;
	private CanvasAltTextAudit canvasAltTextAudit;
	private IframeAltTextAudit iframeAltTextAudit;
	private ObjectAltTextAudit objectAltTextAudit;
	private SVGAltTextAudit svgAltTextAudit;
	private ParagraphingAudit paragraphAudit;
	private ReadabilityAudit readabilityAudit;
	private PubSubAuditUpdatePublisherImpl auditUpdateTopic;

	@Before
	public void setUp() throws Exception {
		controller = new AuditController();
		auditRecordService = mock(AuditRecordService.class);
		pageStateService = mock(PageStateService.class);
		imageAltTextAudit = mock(ImageAltTextAudit.class);
		appletAltTextAudit = mock(AppletAltTextAudit.class);
		canvasAltTextAudit = mock(CanvasAltTextAudit.class);
		iframeAltTextAudit = mock(IframeAltTextAudit.class);
		objectAltTextAudit = mock(ObjectAltTextAudit.class);
		svgAltTextAudit = mock(SVGAltTextAudit.class);
		paragraphAudit = mock(ParagraphingAudit.class);
		readabilityAudit = mock(ReadabilityAudit.class);
		auditUpdateTopic = mock(PubSubAuditUpdatePublisherImpl.class);

		setField("audit_record_service", auditRecordService);
		setField("page_state_service", pageStateService);
		setField("image_alt_text_auditor", imageAltTextAudit);
		setField("appletAllAltTextAudit", appletAltTextAudit);
		setField("canvasAltTextAudit", canvasAltTextAudit);
		setField("iframeAltTextAudit", iframeAltTextAudit);
		setField("objectAltTextAudit", objectAltTextAudit);
		setField("svgAltTextAudit", svgAltTextAudit);
		setField("paragraph_auditor", paragraphAudit);
		setField("readability_auditor", readabilityAudit);
		setField("audit_update_topic", auditUpdateTopic);
	}

	private void setField(String fieldName, Object value) throws Exception {
		Field field = AuditController.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(controller, value);
	}

	@Test
	public void acknowledgeInvalidMessageReturnsOkStatusAndReasonBody() throws Exception {
		Method method = AuditController.class.getDeclaredMethod("acknowledgeInvalidMessage", String.class);
		method.setAccessible(true);

		@SuppressWarnings("unchecked")
		ResponseEntity<String> response = (ResponseEntity<String>) method.invoke(controller, "Invalid pageAuditId");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pageAuditId", response.getBody());
	}

	@Test
	public void auditAlreadyExistsReturnsTrueWhenAuditNameMatches() throws Exception {
		Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
		method.setAccessible(true);

		Audit existing = mock(Audit.class);
		when(existing.getName()).thenReturn(AuditName.ALT_TEXT);

		Audit other = mock(Audit.class);
		when(other.getName()).thenReturn(AuditName.PARAGRAPHING);

		Set<Audit> audits = new HashSet<>();
		audits.add(existing);
		audits.add(other);

		Boolean result = (Boolean) method.invoke(controller, audits, AuditName.ALT_TEXT);

		assertTrue(result);
	}

	@Test
	public void auditAlreadyExistsReturnsFalseWhenNoAuditNameMatches() throws Exception {
		Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
		method.setAccessible(true);

		Audit existing = mock(Audit.class);
		when(existing.getName()).thenReturn(AuditName.READING_COMPLEXITY);

		Set<Audit> audits = new HashSet<>();
		audits.add(existing);

		Boolean result = (Boolean) method.invoke(controller, audits, AuditName.ALT_TEXT);

		assertFalse(result);
	}

	@Test
	public void auditAlreadyExistsReturnsFalseForEmptySet() throws Exception {
		Method method = AuditController.class.getDeclaredMethod("auditAlreadyExists", Set.class, AuditName.class);
		method.setAccessible(true);

		Set<Audit> audits = new HashSet<>();
		Boolean result = (Boolean) method.invoke(controller, audits, AuditName.ALT_TEXT);

		assertFalse(result);
	}

	@Test
	public void receiveMessageWithNullBodyReturnsOk() {
		ResponseEntity<String> response = controller.receiveMessage(null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pubsub payload", response.getBody());
	}

	@Test
	public void receiveMessageWithNullMessageReturnsOk() {
		Body body = mock(Body.class);
		when(body.getMessage()).thenReturn(null);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pubsub payload", response.getBody());
	}

	@Test
	public void receiveMessageWithNullDataReturnsOk() {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(null);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pubsub payload", response.getBody());
	}

	@Test
	public void receiveMessageWithBlankDataReturnsOk() {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn("   ");

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Empty pubsub payload data", response.getBody());
	}

	@Test
	public void receiveMessageWithInvalidBase64ReturnsOk() {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn("not-valid-base64!!!");

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pubsub message format", response.getBody());
	}

	@Test
	public void receiveMessageWithInvalidJsonReturnsOk() {
		String invalidJson = Base64.getEncoder().encodeToString("not json".getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(invalidJson);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pubsub message format", response.getBody());
	}

	@Test
	public void receiveMessageWithZeroPageAuditIdReturnsOk() {
		String json = "{\"pageAuditId\":0,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pageAuditId", response.getBody());
	}

	@Test
	public void receiveMessageWithNegativePageAuditIdReturnsOk() {
		String json = "{\"pageAuditId\":-1,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Invalid pageAuditId", response.getBody());
	}

	@Test
	public void receiveMessageWithAuditRecordNotFoundReturnsOk() {
		String json = "{\"pageAuditId\":42,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		when(auditRecordService.findById(42L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Audit record not found", response.getBody());
	}

	@Test
	public void receiveMessageWithPageStateNotFoundReturnsOk() {
		String json = "{\"pageAuditId\":42,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		AuditRecord auditRecord = mock(AuditRecord.class);
		when(auditRecordService.findById(42L)).thenReturn(Optional.of(auditRecord));
		when(pageStateService.findByAuditRecordId(42L)).thenReturn(null);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Page state not found", response.getBody());
	}

	@Test
	public void receiveMessageSuccessfulAuditReturnsOk() throws Exception {
		String json = "{\"pageAuditId\":42,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		AuditRecord auditRecord = mock(AuditRecord.class);
		when(auditRecord.getId()).thenReturn(42L);
		when(auditRecordService.findById(42L)).thenReturn(Optional.of(auditRecord));

		PageState pageState = mock(PageState.class);
		when(pageState.getId()).thenReturn(100L);
		List<ElementState> elements = new ArrayList<>();
		when(pageState.getElements()).thenReturn(elements);
		when(pageStateService.findByAuditRecordId(42L)).thenReturn(pageState);
		when(pageStateService.getElementStates(100L)).thenReturn(elements);

		when(auditRecordService.getAllAudits(42L)).thenReturn(new HashSet<>());

		Audit mockAudit = mock(Audit.class);
		when(mockAudit.getId()).thenReturn(1L);
		when(imageAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(appletAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(canvasAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(iframeAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(objectAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(svgAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(readabilityAudit.execute(any(), any(), any())).thenReturn(mockAudit);
		when(paragraphAudit.execute(any(), any(), any())).thenReturn(mockAudit);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Successfully completed content audit", response.getBody());
	}

	@Test
	public void receiveMessageSkipsExistingAudits() throws Exception {
		String json = "{\"pageAuditId\":42,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		AuditRecord auditRecord = mock(AuditRecord.class);
		when(auditRecord.getId()).thenReturn(42L);
		when(auditRecordService.findById(42L)).thenReturn(Optional.of(auditRecord));

		PageState pageState = mock(PageState.class);
		when(pageState.getId()).thenReturn(100L);
		List<ElementState> elements = new ArrayList<>();
		when(pageState.getElements()).thenReturn(elements);
		when(pageStateService.findByAuditRecordId(42L)).thenReturn(pageState);
		when(pageStateService.getElementStates(100L)).thenReturn(elements);

		Set<Audit> existingAudits = new HashSet<>();
		Audit altTextAudit = mock(Audit.class);
		when(altTextAudit.getName()).thenReturn(AuditName.ALT_TEXT);
		existingAudits.add(altTextAudit);
		Audit readingAudit = mock(Audit.class);
		when(readingAudit.getName()).thenReturn(AuditName.READING_COMPLEXITY);
		existingAudits.add(readingAudit);
		Audit paragraphingAudit = mock(Audit.class);
		when(paragraphingAudit.getName()).thenReturn(AuditName.PARAGRAPHING);
		existingAudits.add(paragraphingAudit);

		when(auditRecordService.getAllAudits(42L)).thenReturn(existingAudits);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(imageAltTextAudit, never()).execute(any(), any(), any());
		verify(readabilityAudit, never()).execute(any(), any(), any());
		verify(paragraphAudit, never()).execute(any(), any(), any());
	}

	@Test
	public void receiveMessageExceptionDuringAuditReturns500() {
		String json = "{\"pageAuditId\":42,\"accountId\":1}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(encoded);

		AuditRecord auditRecord = mock(AuditRecord.class);
		when(auditRecord.getId()).thenReturn(42L);
		when(auditRecordService.findById(42L)).thenReturn(Optional.of(auditRecord));

		PageState pageState = mock(PageState.class);
		when(pageState.getId()).thenReturn(100L);
		List<ElementState> elements = new ArrayList<>();
		when(pageState.getElements()).thenReturn(elements);
		when(pageStateService.findByAuditRecordId(42L)).thenReturn(pageState);
		when(pageStateService.getElementStates(100L)).thenReturn(elements);

		when(auditRecordService.getAllAudits(42L)).thenThrow(new RuntimeException("DB error"));

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Error performing content audit", response.getBody());
	}
}
