package com.looksee.visualDesignAudit;

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
import java.util.Base64;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.visualDesignAudit.audit.ImageAudit;
import com.looksee.visualDesignAudit.audit.ImagePolicyAudit;
import com.looksee.visualDesignAudit.audit.NonTextColorContrastAudit;
import com.looksee.visualDesignAudit.audit.TextColorContrastAudit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;

	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private TextColorContrastAudit text_contrast_audit_impl;
	
	@Autowired
	private NonTextColorContrastAudit non_text_contrast_audit_impl;
	
	@Autowired
	private ImageAudit image_audit;

	@Autowired
	private ImagePolicyAudit image_policy_audit;
	
	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;
	
	/**
	 * Receives a {@linkplain PageAuditMessage} from Pub/Sub and executes the visual design audit
	 * 
	 * @param body {@linkplain Body} containing the {@linkplain PageAuditMessage}
	 * @return {@linkplain ResponseEntity} containing the result of the audit
	 * @throws JsonMappingException if the JSON mapping fails
	 * @throws JsonProcessingException if the JSON processing fails
	 * @throws ExecutionException if the execution fails
	 * @throws InterruptedException if the thread is interrupted
	 */
	@Operation(
		summary = "Execute visual design audit",
		description = "Receives a PageAuditMessage from Pub/Sub and executes the visual design audit including text and non-text color contrast analysis",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "Successfully completed visual design audit",
				content = @Content(
					mediaType = "text/plain",
					schema = @Schema(example = "Successfully completed visual design audit")
				)
			),
			@ApiResponse(
				responseCode = "400",
				description = "Bad request - invalid message format or data",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(description = "Error response object")
				)
			),
			@ApiResponse(
				responseCode = "500",
				description = "Internal server error during audit execution",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(description = "Error response object")
				)
			)
		},
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Pub/Sub message containing PageAuditMessage",
			required = true,
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(description = "Pub/Sub message wrapper")
			)
		)
	)
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("page audit msg received = "+target);

	    ObjectMapper mapper = new ObjectMapper();
	    PageAuditMessage audit_record_msg = mapper.readValue(target, PageAuditMessage.class);
    
    	Domain domain = domain_service.findByAuditRecord(audit_record_msg.getPageAuditId());
    	
		DesignSystem design_system = buildDefaultDesignSystem();
		if(domain != null) {
			design_system = domain_service.getDesignSystem(domain.getId()).get();
		}

		AuditRecord audit_record = audit_record_service.findById(audit_record_msg.getPageAuditId()).get();
		PageState page = page_state_service.getPageStateForAuditRecord(audit_record.getId());
		List<ElementState> elements = page_state_service.getElementStates(page.getId());
		page.setElements(elements);

		Set<Audit> audits = audit_record_service.getAllAudits(audit_record.getId());

		if(!auditAlreadyExists(audits, AuditName.TEXT_BACKGROUND_CONTRAST)) {
		   	Audit text_contrast_audit = text_contrast_audit_impl.execute(page, audit_record, design_system);
		   	audit_record_service.addAudit(audit_record_msg.getPageAuditId(), text_contrast_audit);
		   	audits.add(text_contrast_audit);
		}

		if(!auditAlreadyExists(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST)) {
			Audit non_text_contrast_audit = non_text_contrast_audit_impl.execute(page, audit_record, design_system);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), non_text_contrast_audit);
			audits.add(non_text_contrast_audit);
		}

		if(!auditAlreadyExists(audits, AuditName.IMAGE_COPYRIGHT)) {
			Audit image_copyright_audit = image_audit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), image_copyright_audit.getId());
		}
		
		if(!auditAlreadyExists(audits, AuditName.IMAGE_POLICY)) {
			Audit image_policy_result = image_policy_audit.execute(page, audit_record, null);
			audit_record_service.addAudit(audit_record_msg.getPageAuditId(), image_policy_result.getId());
		}
		
		AuditProgressUpdate audit_update = new AuditProgressUpdate(audit_record_msg.getAccountId(),
																	1.0, 
																	"Completed visual design audit!",
																	AuditCategory.CONTENT, 
																	AuditLevel.PAGE, 
																	audit_record_msg.getPageAuditId());

		String audit_record_json = mapper.writeValueAsString(audit_update);
		audit_update_topic.publish(audit_record_json);

		return new ResponseEntity<String>("Successfully completed visual design audit", HttpStatus.OK);
	}
	
	/**
	 * Builds a default {@linkplain DesignSystem}
	 * 
	 * @return
	 */
	private DesignSystem buildDefaultDesignSystem() {
		return new DesignSystem();
	}

	/**
	 * Checks if the any of the provided {@link Audit audits} have a name that matches 
	 * 		the provided {@linkplain AuditName}
	 * 
	 * @param audits
	 * @param audit_name
	 * 
	 * @return
	 * 
	 * @pre audits != null
	 * @pre audit_name != null
	 */
	private boolean auditAlreadyExists(Set<Audit> audits, AuditName audit_name) {
		assert audits != null;
		assert audit_name != null;
		
		for(Audit audit : audits) {
			if(audit_name.equals(audit.getName())) {
				return true;
			}
		}
		return false;
	}
}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]