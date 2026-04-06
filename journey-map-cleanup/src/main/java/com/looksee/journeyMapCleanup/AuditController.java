package com.looksee.journeyMapCleanup;

import java.util.Set;

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

import com.looksee.mapper.Body;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.services.DomainMapService;
import com.looksee.services.JourneyService;


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
// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private DomainMapService domain_map_service;
	
	@Autowired
	private JourneyService journey_service;
	
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws Exception
	{
		Set<DomainMap> domain_maps = domain_map_service.getAllMapsWithinLastDay(7);
		
		log.warn("domain_maps = "+domain_maps.size());
		for(DomainMap map : domain_maps){
			log.warn("checking domain map = "+map.getId());
			log.warn("Making sure that updates are actually happening");
			Set<Journey> journey_count = journey_service.getDomainMapJourneys(map.getId());
			log.warn("journeys created within last hour = "+journey_count.size());
			if(journey_count.size() == 0){
				log.warn("changing candidate journeys to error");
				journey_service.changeJourneyStatus(map.getId(), JourneyStatus.CANDIDATE, JourneyStatus.ERROR);
			}
		}

		return new ResponseEntity<String>("Error verifying journey", HttpStatus.OK);
	}
}