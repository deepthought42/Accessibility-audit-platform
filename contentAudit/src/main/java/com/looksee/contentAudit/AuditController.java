package com.looksee.contentAudit;

/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// [START cloudrun_pubsub_handler]
// [START run_pubsub_handler]
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.PageStateService;

/**
 * API controller that performs a content audit.
 *
 * <p><strong>Class Invariants:</strong></p>
 * <ul>
 *   <li>All {@code @Autowired} dependencies must be non-null after Spring initialization</li>
 *   <li>The controller only processes valid Pub/Sub push payloads with base64-encoded {@link PageAuditMessage} JSON</li>
 *   <li>Invalid or malformed messages are acknowledged with HTTP 200 to prevent Pub/Sub redelivery of poison messages</li>
 * </ul>
 */
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ImageAltTextAudit image_alt_text_auditor;

	@Autowired
	private AppletAltTextAudit appletAllAltTextAudit;

	@Autowired
	private CanvasAltTextAudit canvasAltTextAudit;

	@Autowired
	private IframeAltTextAudit iframeAltTextAudit;

	@Autowired
	private ObjectAltTextAudit objectAltTextAudit;

	@Autowired
	private SVGAltTextAudit svgAltTextAudit;

	@Autowired
	private ParagraphingAudit paragraph_auditor;

	@Autowired
	private ReadabilityAudit readability_auditor;

	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;
	
	/**
	 * Receives a message from Pub/Sub and performs a content audit on the page.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code body} should contain a valid Pub/Sub push payload (null/invalid payloads are handled gracefully)</li>
	 *   <li>{@code body.getMessage().getData()} should contain base64-encoded {@link PageAuditMessage} JSON</li>
	 *   <li>The decoded {@code PageAuditMessage} must have a positive {@code pageAuditId}</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null {@link ResponseEntity} with HTTP 200 for valid or gracefully-handled invalid messages</li>
	 *   <li>Returns HTTP 500 only for unexpected internal errors during audit execution</li>
	 *   <li>On success, all applicable audits (alt text, readability, paragraphing) are persisted and linked to the audit record</li>
	 *   <li>An {@link AuditProgressUpdate} message is published to notify downstream systems of completion</li>
	 * </ul>
	 *
	 * @param body the body of the message containing the audit record and page state
	 * @return ResponseEntity containing the result of the audit
	 */
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
		if (body == null || body.getMessage() == null || body.getMessage().getData() == null) {
			log.warn("invalid pubsub payload received");
			return acknowledgeInvalidMessage("Invalid pubsub payload");
		}

		Body.Message message = body.getMessage();
		String data = message.getData();
		if (data.isBlank()) {
			log.warn("received empty pubsub payload data");
			return acknowledgeInvalidMessage("Empty pubsub payload data");
		}

		PageAuditMessage audit_record_msg;
		try {
			String target = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
			ObjectMapper input_mapper = new ObjectMapper();
			audit_record_msg = input_mapper.readValue(target, PageAuditMessage.class);
		} catch (IllegalArgumentException | JsonProcessingException e) {
			log.warn("invalid pubsub message format", e);
			return acknowledgeInvalidMessage("Invalid pubsub message format");
		}

		if (audit_record_msg.getPageAuditId() <= 0) {
			log.warn("invalid pageAuditId in pubsub message: {}", audit_record_msg.getPageAuditId());
			return acknowledgeInvalidMessage("Invalid pageAuditId");
		}
		
		try {
			Optional<AuditRecord> audit_record_optional = audit_record_service.findById(audit_record_msg.getPageAuditId());
			if (audit_record_optional.isEmpty()) {
				log.warn("audit record not found for page audit id {}", audit_record_msg.getPageAuditId());
				return acknowledgeInvalidMessage("Audit record not found");
			}

			AuditRecord audit_record = audit_record_optional.get();
			PageState page = page_state_service.findByAuditRecordId(audit_record_msg.getPageAuditId());
			if (page == null) {
				log.warn("page state not found for page audit id {}", audit_record_msg.getPageAuditId());
				return acknowledgeInvalidMessage("Page state not found");
			}
			page.setElements(page_state_service.getElementStates(page.getId()));
			log.warn("evaluating "+page.getElements().size()+" element state for content audit with page ID :: "+page.getId());
			Set<Audit> audits = audit_record_service.getAllAudits(audit_record.getId());

			if(!auditAlreadyExists(audits, AuditName.ALT_TEXT)) {
				Audit img_alt_text_audit = image_alt_text_auditor.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), img_alt_text_audit.getId());

				Audit applet_alt_text_audit = appletAllAltTextAudit.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), applet_alt_text_audit.getId());

				Audit canvas_alt_text_audit = canvasAltTextAudit.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), canvas_alt_text_audit.getId());

				Audit iframe_alt_text_audit = iframeAltTextAudit.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), iframe_alt_text_audit.getId());

				Audit object_alt_text_audit = objectAltTextAudit.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), object_alt_text_audit.getId());

				Audit svg_alt_text_audit = svgAltTextAudit.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), svg_alt_text_audit.getId());
			}

			if(!auditAlreadyExists(audits, AuditName.READING_COMPLEXITY)) {
				Audit readability_audit = readability_auditor.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), readability_audit.getId());
			}

			if(!auditAlreadyExists(audits, AuditName.PARAGRAPHING)) {
				Audit paragraph_audit = paragraph_auditor.execute(page, audit_record, null);
				audit_record_service.addAudit(audit_record_msg.getPageAuditId(), paragraph_audit.getId());
			}
		} catch (Exception e) {
			log.error("exception caught during content audit", e);
			return new ResponseEntity<String>("Error performing content audit", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
		AuditProgressUpdate audit_update = new AuditProgressUpdate(audit_record_msg.getAccountId(),
												1.0, 
												"Content Audit Complete!",
														AuditCategory.CONTENT,
														AuditLevel.PAGE,
														audit_record_msg.getPageAuditId());

		try {
			String audit_record_json = mapper.writeValueAsString(audit_update);
			audit_update_topic.publish(audit_record_json);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("interrupted while publishing audit progress update", e);
			return new ResponseEntity<String>("Error publishing audit progress", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (JsonProcessingException | java.util.concurrent.ExecutionException e) {
			log.error("failed to publish audit progress update", e);
			return new ResponseEntity<String>("Error publishing audit progress", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<String>("Successfully completed content audit", HttpStatus.OK);
	}
	
	/**
	 * Acknowledges an invalid Pub/Sub message by returning HTTP 200 to prevent redelivery.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code reason} must not be null</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null {@link ResponseEntity} with HTTP 200 status</li>
	 * </ul>
	 *
	 * @param reason the reason the message is invalid, must not be null
	 * @return ResponseEntity with HTTP 200 and the reason as body
	 * @throws NullPointerException if {@code reason} is null
	 */
	private ResponseEntity<String> acknowledgeInvalidMessage(String reason) {
		Objects.requireNonNull(reason, "reason must not be null");
		return new ResponseEntity<String>(reason, HttpStatus.OK);
	}

	/**
	 * Checks if any of the provided {@link Audit audits} have a name that matches
	 * the provided {@linkplain AuditName}.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code audits} must not be null</li>
	 *   <li>{@code audit_name} must not be null</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns {@code true} if any audit in the set has a matching name, {@code false} otherwise</li>
	 *   <li>The input set is not modified</li>
	 * </ul>
	 *
	 * @param audits the set of existing audits to search, must not be null
	 * @param audit_name the audit name to search for, must not be null
	 * @return {@code true} if a matching audit exists, {@code false} otherwise
	 * @throws NullPointerException if {@code audits} or {@code audit_name} is null
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
