package com.looksee.frontEndBroadcaster;

import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.dto.PageStateDto;
import com.looksee.mapper.Body;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.frontEndBroadcaster.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.frontEndBroadcaster.services.AccountService;
import com.looksee.services.PageStateService;

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
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private MessageBroadcaster broadcaster;

	@Autowired
	private AccountService account_service;

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws Exception
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
		String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
		ObjectMapper input_mapper = new ObjectMapper();
		try{
			PageBuiltMessage page_built_msg = input_mapper.readValue(target, PageBuiltMessage.class);
			log.info("{}", "Page Built Message received : "+target);

			Optional<PageState> page = page_state_service.findById(page_built_msg.getPageId());
			if(page.isPresent()){
				PageStateDto page_state_dto = new PageStateDto(page_built_msg.getAuditRecordId(), page.get());
				Optional<Account> account = account_service.findById(page_built_msg.getAccountId());

				if(account.isPresent()){
					broadcaster.broadcastPageState(account.get().getUserId().replace("|",""), page_state_dto);
				}
				else{
					log.warn("Account is NOT present");
				}
				
				return new ResponseEntity<String>("Page found messages successfully sent to user", HttpStatus.OK);
			}
			else {
				log.warn("Page is NOT present");
			}
		}catch(Exception e){}

		try{
			AuditProgressUpdate audit_update_message = input_mapper.readValue(target, AuditProgressUpdate.class);
			Optional<Account> account = account_service.findById(audit_update_message.getAccountId());

			if(account.isPresent()){
				broadcaster.broadcastAuditUpdate(account.get().getUserId().replace("|",""), audit_update_message);
				return new ResponseEntity<String>("Page found messages successfully sent to user", HttpStatus.OK);
			}
			else {
				log.warn("Account 2 is NOT present");
			}
		}catch(Exception e){e.printStackTrace();}
		
		return null;
	}
}