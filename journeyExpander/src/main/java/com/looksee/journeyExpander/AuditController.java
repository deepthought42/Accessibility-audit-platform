/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.looksee.journeyExpander;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.messaging.web.PubSubAuditController;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.enums.Action;
import com.looksee.browsing.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;

/**
 * Receives verified journeys via Pub/Sub push and expands them into candidate
 * journeys by appending interactive-element click steps to the journey's
 * resulting page.
 *
 * <p>This controller used to reimplement the standard Pub/Sub envelope
 * decoding, idempotency check, metrics emission, and trace propagation
 * boilerplate by hand. Wave 3.1 of the architecture review centralized that
 * boilerplate in {@link PubSubAuditController}, leaving this class focused on
 * the actual journey-expansion business logic.</p>
 */
@RestController
public class AuditController extends PubSubAuditController<VerifiedJourneyMessage> {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    @Autowired private DomainService domain_service;
    @Autowired private JourneyService journey_service;
    @Autowired private DomainMapService domain_map_service;
    @Autowired private AuditRecordService audit_record_service;
    @Autowired private PageStateService page_state_service;
    @Autowired private StepService step_service;
    @Autowired private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;

    @Override
    protected String serviceName() {
        return "journey-expander";
    }

    @Override
    protected String topicName() {
        return "journey_verified";
    }

    @Override
    protected Class<VerifiedJourneyMessage> payloadType() {
        return VerifiedJourneyMessage.class;
    }

    @Override
    @Transactional
    protected void handle(VerifiedJourneyMessage journey_msg) throws Exception {
        Journey journey = journey_msg.getJourney();
        if (journey == null || journey.getSteps() == null || journey.getSteps().isEmpty()) {
            log.warn("IGNORING JOURNEY! journey or journey steps missing");
            return;
        }

        if (!shouldBeExpanded(journey)) {
            log.warn("IGNORING JOURNEY! journey should not be expanded");
            return;
        }

        List<Step> journey_steps = journey.getSteps();
        Step last_step = journey_steps.get(journey_steps.size() - 1);
        PageState journey_result_page = last_step instanceof LandingStep
            ? last_step.getStartPage()
            : last_step.getEndPage();
        if (journey_result_page == null
                || journey_result_page.getUrl() == null
                || journey_result_page.getUrl().isBlank()) {
            log.warn("IGNORING JOURNEY! journey result page or url missing");
            return;
        }

        Domain domain = domain_service.findByAuditRecord(journey_msg.getAuditRecordId());
        if (domain == null) {
            log.warn("IGNORING JOURNEY! no domain found for audit record {}",
                journey_msg.getAuditRecordId());
            return;
        }
        if (BrowserUtils.isExternalLink(domain.getUrl(), journey_result_page.getUrl())) {
            log.info("Last page of journey is external; not expanding");
            return;
        }

        DomainMap domain_map = domain_map_service.findByDomainAuditId(journey_msg.getAuditRecordId());
        if (domain_map != null) {
            List<Step> page_steps = step_service.getStepsWithStartPage(journey_result_page, domain_map.getId());
            if (page_steps != null && page_steps.size() > 1) {
                log.info("Steps already exist for page {}; skipping expansion",
                    journey_result_page.getKey());
                return;
            }
        }

        List<Action> actions = List.of(Action.CLICK);
        List<ElementState> page_elements = page_state_service.getElementStates(journey_result_page.getId());
        List<ElementState> leaf_elements = page_elements == null
            ? new ArrayList<>()
            : new ArrayList<>(page_elements);
        journey_result_page.setElements(leaf_elements);
        log.info("{} leaf elements found for url={}", leaf_elements.size(), journey_result_page.getUrl());

        leaf_elements.removeIf(element -> element == null
            || BrowserService.isStructureTag(element.getName())
            || !ElementStateUtils.isInteractiveElement(element));
        log.info("{} leaf elements after filtering", leaf_elements.size());

        int journey_cnt = 0;
        for (ElementState leaf_element : leaf_elements) {
            for (Action action : actions) {
                Step step = new SimpleStep(journey_result_page,
                    leaf_element,
                    action,
                    "",
                    null,
                    JourneyStatus.CANDIDATE);

                if (existsInJourney(journey, step)) {
                    continue;
                }

                if (domain_map == null) {
                    domain_map = domain_map_service.save(new DomainMap());
                    audit_record_service.addDomainMap(journey_msg.getAuditRecordId(), domain_map.getId());
                }

                Step step_record = step_service.findByCandidateKey(step.getKey(), domain_map.getId());
                if (step_record != null) {
                    continue;
                }

                step = step_service.save(step);
                List<Step> steps = new ArrayList<>(journey_steps);
                steps.add(step);

                Journey expanded_journey = new Journey(steps, JourneyStatus.CANDIDATE);
                Journey journey_record = journey_service.findByCandidateKey(expanded_journey.getCandidateKey());
                if (journey_record != null) {
                    continue;
                }

                journey_record = journey_service.save(domain_map.getId(), expanded_journey);
                long journey_id = journey_record.getId();
                journey_record.setSteps(steps);
                steps.forEach(temp_step -> journey_service.addStep(journey_id, temp_step.getId()));
                domain_map_service.addJourneyToDomainMap(journey_record.getId(), domain_map.getId());

                JourneyCandidateMessage candidate = new JourneyCandidateMessage(journey_record,
                    BrowserType.CHROME,
                    journey_msg.getAccountId(),
                    journey_msg.getAuditRecordId(),
                    domain_map.getId());
                String candidate_json = JacksonConfig.mapper().writeValueAsString(candidate);
                journey_candidate_topic.publish(candidate_json);
                journey_cnt++;
            }
        }

        log.info("generated {} journeys to explore", journey_cnt);
    }

    private boolean shouldBeExpanded(Journey journey) {
        assert journey != null : "Precondition violated: journey must not be null";
        if (journey.getSteps() == null || journey.getSteps().isEmpty()) {
            return false;
        }
        Step last_step = journey.getSteps().get(journey.getSteps().size() - 1);
        if (last_step == null) {
            return false;
        }
        if (last_step instanceof LandingStep) {
            return last_step.getStartPage() != null;
        } else if (last_step instanceof SimpleStep) {
            if (last_step.getStartPage() == null || last_step.getEndPage() == null) {
                return false;
            }
            return !Objects.equals(last_step.getStartPage().getKey(), last_step.getEndPage().getKey());
        }
        return false;
    }

    private boolean existsInJourney(Journey journey, Step step) {
        assert journey != null : "Precondition violated: journey must not be null";
        assert step != null : "Precondition violated: step must not be null";
        if (journey.getSteps() == null) {
            return false;
        }
        for (Step journey_step : journey.getSteps()) {
            if (journey_step == null) {
                continue;
            }
            if (step instanceof LandingStep && journey_step instanceof LandingStep) {
                if (step.getStartPage() != null
                        && journey_step.getStartPage() != null
                        && Objects.equals(step.getStartPage().getKey(), journey_step.getStartPage().getKey())) {
                    return true;
                }
            } else if (step instanceof SimpleStep && journey_step instanceof SimpleStep) {
                SimpleStep temp1 = (SimpleStep) step;
                SimpleStep temp2 = (SimpleStep) journey_step;
                if (temp1.getStartPage() != null
                        && temp2.getStartPage() != null
                        && temp1.getElementState() != null
                        && temp2.getElementState() != null
                        && Objects.equals(temp1.getStartPage().getUrl(), temp2.getStartPage().getUrl())
                        && Objects.equals(temp1.getElementState().getKey(), temp2.getElementState().getKey())
                        && Objects.equals(temp1.getAction(), temp2.getAction())
                        && Objects.equals(temp1.getActionInput(), temp2.getActionInput())) {
                    return true;
                }
            }
        }
        return false;
    }
}
