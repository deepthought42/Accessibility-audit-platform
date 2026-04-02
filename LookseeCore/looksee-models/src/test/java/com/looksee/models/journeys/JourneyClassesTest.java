package com.looksee.models.journeys;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.JourneyStatus;

/**
 * Unit tests for journey model classes.
 */
class JourneyClassesTest {

    // ===== Journey =====
    @Test
    void journeyDefaultConstructor() {
        Journey journey = new Journey();
        assertNotNull(journey);
        assertNotNull(journey.getSteps());
        assertNotNull(journey.getOrderedIds());
        assertNotNull(journey.getKey());
    }

    @Test
    void journeyParameterizedConstructor() {
        List<Step> steps = new ArrayList<>();
        Journey journey = new Journey(steps, JourneyStatus.CANDIDATE);
        assertEquals(JourneyStatus.CANDIDATE, journey.getStatus());
        assertNotNull(journey.getSteps());
        assertNotNull(journey.getKey());
        assertNotNull(journey.getCandidateKey());
    }

    @Test
    void journeySetStatus() {
        Journey journey = new Journey();
        journey.setStatus(JourneyStatus.VERIFIED);
        assertEquals(JourneyStatus.VERIFIED, journey.getStatus());
    }

    @Test
    void journeyGenerateKey() {
        Journey journey = new Journey();
        String key = journey.generateKey();
        assertNotNull(key);
        assertTrue(key.startsWith("journey"));
    }

    @Test
    void journeyAddStep() {
        Journey journey = new Journey();
        SimpleStep step = new SimpleStep();
        assertTrue(journey.addStep(step));
        assertEquals(1, journey.getSteps().size());
    }

    // ===== DomainMap =====
    @Test
    void domainMapDefaultConstructor() {
        DomainMap domainMap = new DomainMap();
        assertNotNull(domainMap);
    }

    // ===== SimpleStep =====
    @Test
    void simpleStepDefaultConstructor() {
        SimpleStep step = new SimpleStep();
        assertNotNull(step);
    }

    // ===== LoginStep =====
    @Test
    void loginStepDefaultConstructor() {
        LoginStep step = new LoginStep();
        assertNotNull(step);
    }

    // ===== LandingStep =====
    @Test
    void landingStepDefaultConstructor() {
        LandingStep step = new LandingStep();
        assertNotNull(step);
    }

    // ===== Redirect =====
    @Test
    void redirectDefaultConstructor() {
        Redirect redirect = new Redirect();
        assertNotNull(redirect);
    }
}
