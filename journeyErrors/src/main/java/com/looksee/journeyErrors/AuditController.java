package com.looksee.journeyErrors;

import java.util.Base64;
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
import com.looksee.journeyErrors.mapper.Body;
import com.looksee.journeyErrors.models.enums.JourneyStatus;
import com.looksee.journeyErrors.models.journeys.Journey;
import com.looksee.journeyErrors.models.message.JourneyCandidateMessage;
import com.looksee.journeyErrors.services.JourneyService;

/*
 * Copyright 2019 Look-see Inc
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
	private JourneyService journey_service;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws Exception 
	{	
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("processing journey dead letter message = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    JourneyCandidateMessage journey_msg = input_mapper.readValue(target, JourneyCandidateMessage.class);
	    
	    //JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
		Journey journey = journey_msg.getJourney();
		
		log.warn("processing journey with id = "+journey.getId()+"; with status = "+journey.getStatus());
	    //CHECK IF JOURNEY WITH CANDIDATE KEY HAS ALREADY BEEN EVALUATED
	    if(!JourneyStatus.CANDIDATE.equals(journey.getStatus())) {
	    	log.warn("Journey has already been verified or discarded, or is being evaluated with status = "+journey.getStatus());
	    	return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
	    }
	    
	    //update journey status to ERROR
	    journey_service.updateStatus(journey.getId(), JourneyStatus.ERROR);
		
		return new ResponseEntity<String>("Successfully expanded journey", HttpStatus.OK);
		
	}

}