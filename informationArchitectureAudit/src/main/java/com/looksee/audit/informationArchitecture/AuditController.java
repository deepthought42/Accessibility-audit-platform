package com.looksee.audit.informationArchitecture;

import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * REST controller that receives Pub/Sub audit messages, orchestrates page-level
 * information architecture and accessibility audits, and publishes completion updates.
 *
 * <h3>Class Invariant</h3>
 * <ul>
 *   <li>All {@code @Autowired} audit and service dependencies are non-null after construction.</li>
 *   <li>Each audit execution produces a persisted {@link Audit} with a valid id before the
 *       controller registers it against the {@link AuditRecord}.</li>
 * </ul>
 */
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private HeaderStructureAudit header_structure_auditor;

	@Autowired
	private TableStructureAudit table_structure_auditor;
	
	@Autowired
	private FormStructureAudit form_structure_auditor;

	@Autowired
	private OrientationAudit orientationAudit;

	@Autowired
	private InputPurposeAudit inputPurposeAudit;

	@Autowired
	private IdentifyPurposeAudit identifyPurposeAudit;

	@Autowired
	private UseOfColorAudit useOfColorAudit;

	@Autowired
	private ReflowAudit reflowAudit;

	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private AudioControlAudit audioControlAudit;

	@Autowired
	private VisualPresentationAudit visualPresentationAudit;

	@Autowired
	private PageLanguageAudit pageLanguageAudit;

	@Autowired
	private MetadataAudit metadata_auditor;

	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private TextSpacingAudit textSpacingAudit;

	@Autowired
	private SecurityAudit security_auditor;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;

	@Autowired
	private IdempotencyService idempotencyService;
	
	/**
	 * Receives a Pub/Sub message containing a {@link PageAuditMessage}, executes all
	 * registered information architecture audits against the referenced page, and
	 * publishes a completion update.
	 *
	 * @param body the Pub/Sub push message wrapper; must not be {@code null}
	 * @return {@code 200 OK} on success, {@code 400 BAD_REQUEST} for invalid input,
	 *         {@code 404 NOT_FOUND} if the audit record does not exist,
	 *         {@code 500 INTERNAL_SERVER_ERROR} on serialisation failures
	 *
	 * @pre {@code body != null && body.getMessage() != null}
	 * @pre {@code body.getMessage().getData()} is a valid Base64-encoded JSON string
	 *      representing a {@link PageAuditMessage}
	 * @post an {@link AuditProgressUpdate} with progress {@code 1.0} is published to the
	 *       audit update topic
	 * @post every audit that did not already exist for the record has been persisted and
	 *       associated with the {@link AuditRecord}
	 */
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws ExecutionException, InterruptedException
	{
		if(body == null || body.getMessage() == null) {
			return new ResponseEntity<String>("Invalid Pub/Sub message: body.message is required", HttpStatus.BAD_REQUEST);
		}

		if (idempotencyService.isAlreadyProcessed(body.getMessage().getMessageId(), "information-architecture-audit")) {
			return ResponseEntity.ok("Duplicate message, already processed");
		}

		Body.Message message = body.getMessage();
		String data = message.getData();
		if(data == null || data.isEmpty()) {
			return new ResponseEntity<String>("Invalid Pub/Sub message: message.data is required", HttpStatus.BAD_REQUEST);
		}

		String target;
		try {
			target = new String(Base64.getDecoder().decode(data));
		}
		catch(IllegalArgumentException e) {
			log.warn("Invalid Pub/Sub message data encoding", e);
			return new ResponseEntity<String>("Invalid Pub/Sub message: message.data must be base64 encoded", HttpStatus.OK);
		}

		if(target.isEmpty()) {
			return new ResponseEntity<String>("Invalid Pub/Sub message: decoded message.data is empty", HttpStatus.OK);
		}
		log.warn("page audit msg received = "+target);

		PageAuditMessage audit_record_msg;
		try {
			audit_record_msg = JacksonConfig.mapper().readValue(target, PageAuditMessage.class);
		}
		catch(JsonProcessingException e) {
			log.warn("Invalid Pub/Sub message payload", e);
			return new ResponseEntity<String>("Invalid Pub/Sub message: payload must be valid PageAuditMessage JSON", HttpStatus.OK);
		}

		AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).orElse(null);
		if(audit_record == null) {
			return new ResponseEntity<String>("Audit record not found for id: " + audit_record_msg.getPageAuditId(), HttpStatus.NOT_FOUND);
		}
    	PageState page = page_state_service.getPageStateForAuditRecord(audit_record.getId());
    	
    	Set<Audit> audits = audit_record_service.getAllAudits(audit_record.getId());
    	
		//WCAG 2.1 Section 1.3.1 - Structure (headers)
		if(!auditAlreadyExists(audits, AuditName.HEADER_STRUCTURE)) {
			Audit header_structure_audit = header_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), header_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TABLE_STRUCTURE)) {
			Audit table_structure_audit = table_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), table_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.FORM_STRUCTURE)) {
			Audit form_structure_audit = form_structure_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), form_structure_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.ORIENTATION)) {
			Audit orientation_audit = orientationAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), orientation_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.INPUT_PURPOSE)) {
			Audit input_purpose_audit = inputPurposeAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), input_purpose_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.IDENTIFY_PURPOSE)) {
			Audit identify_purpose_audit = identifyPurposeAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), identify_purpose_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.USE_OF_COLOR)) {
			Audit use_of_color_audit = useOfColorAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), use_of_color_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.AUDIO_CONTROL)) {
			Audit audio_control_audit = audioControlAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), audio_control_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.VISUAL_PRESENTATION)) {
			Audit visual_presentation_audit = visualPresentationAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), visual_presentation_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.REFLOW)) {
			Audit reflow_audit = reflowAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), reflow_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TEXT_SPACING)) {
			Audit text_spacing_audit = textSpacingAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), text_spacing_audit.getId());
		}

		/*********************************
			WCAG 2.1 SECTION 3
		**********************************/
		if(!auditAlreadyExists(audits, AuditName.PAGE_LANGUAGE)) {
			Audit page_language_audit = pageLanguageAudit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), page_language_audit.getId());
		}


		//************************************************
		//Original UX audits section
		//*************************************************

		if(!auditAlreadyExists(audits, AuditName.LINKS)) {
			Audit link_audit = links_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), link_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.TITLES)) {
    		Audit title_and_headers = title_and_header_auditor.execute(page, audit_record, null);
    		audit_record_service.addAudit(audit_record_msg.getPageAuditId(), title_and_headers.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.ENCRYPTED)) {
			Audit security_audit = security_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), security_audit.getId());
		}

		if(!auditAlreadyExists(audits, AuditName.METADATA)) {
			Audit metadata = metadata_auditor.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), metadata.getId());
		}
		
		AuditProgressUpdate audit_update = new AuditProgressUpdate(audit_record_msg.getAccountId(),
															1.0,
															"Completed information architecture audit",
															AuditCategory.INFORMATION_ARCHITECTURE,
															AuditLevel.PAGE,
															audit_record_msg.getPageAuditId());

		String audit_record_json;
		try {
			audit_record_json = JacksonConfig.mapper().writeValueAsString(audit_update);
		}
		catch(JsonProcessingException e) {
			log.error("Failed to serialize audit progress update", e);
			return new ResponseEntity<String>("Failed to serialize audit progress update", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			audit_update_topic.publish(audit_record_json);
		}
		catch(Exception e) {
			log.error("Failed to publish audit progress update", e);
			return new ResponseEntity<String>("Failed to publish audit progress update", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		idempotencyService.markProcessed(body.getMessage().getMessageId(), "information-architecture-audit");
		return new ResponseEntity<String>("Successfully audited information architecture", HttpStatus.OK);
	}

	/**
	 * Checks whether any of the provided {@link Audit audits} already have the given
	 * {@link AuditName}.
	 *
	 * @param audits     the set of existing audits to search; must not be {@code null}
	 * @param audit_name the audit name to look for; must not be {@code null}
	 * @return {@code true} if a matching audit exists, {@code false} otherwise
	 *
	 * @pre {@code audits != null}
	 * @pre {@code audit_name != null}
	 * @post result is {@code true} if and only if at least one element in {@code audits}
	 *       has a name equal to {@code audit_name}
	 */
	private boolean auditAlreadyExists(Set<Audit> audits, AuditName audit_name) {
		Objects.requireNonNull(audits, "audits must not be null");
		Objects.requireNonNull(audit_name, "audit_name must not be null");

		for(Audit audit : audits) {
			if(audit_name.equals(audit.getName())) {
				return true;
			}
		}
		return false;
	}
}
