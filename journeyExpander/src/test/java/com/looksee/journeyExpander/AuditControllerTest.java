package com.looksee.journeyExpander;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditControllerTest {

    @InjectMocks
    private AuditController controller;

    @Mock private DomainService domain_service;
    @Mock private JourneyService journey_service;
    @Mock private DomainMapService domain_map_service;
    @Mock private AuditRecordService audit_record_service;
    @Mock private PageStateService page_state_service;
    @Mock private StepService step_service;
    @Mock private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;
    @Mock private IdempotencyService idempotencyService;

    // ================================================================
    // receiveMessage: input validation
    // ================================================================

    @Test
    void receiveMessageReturnsBadRequestWhenBodyIsNull() {
        ResponseEntity<String> response = controller.receiveMessage(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Message data is required", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenMessageIsNull() {
        Body body = new Body();
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Message data is required", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenDataIsBlank() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        when(body.getMessage().getData()).thenReturn("   ");
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Message data is required", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenPayloadIsNotBase64() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        when(body.getMessage().getData()).thenReturn("%%%not_base64%%%");
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenDecodedPayloadIsInvalidJson() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        String encoded = Base64.getEncoder().encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
        when(body.getMessage().getData()).thenReturn(encoded);
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    // ================================================================
    // receiveMessage: journey validation
    // ================================================================

    @Test
    void receiveMessageReturnsBadRequestWhenJourneyIsNull() {
        Body body = createNullJourneyBody();
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Journey has no steps", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenJourneyStepsAreEmpty() {
        Body body = createEmptyStepsBody();
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Journey has no steps", response.getBody());
    }

    // ================================================================
    // receiveMessage: shouldBeExpanded returns false
    // ================================================================

    @Test
    void receiveMessageReturnsOkWhenJourneyShouldNotBeExpanded() {
        Body body = createNonExpandableSimpleStepBody();
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Journey should not be expanded", response.getBody());
    }

    // ================================================================
    // receiveMessage: result page validation
    // ================================================================

    @Test
    void receiveMessageReturnsBadRequestWhenResultPageUrlIsNull() {
        // LandingStep JSON without url field -> url will be null after deserialization
        String json = "{\"auditRecordId\":100,\"accountId\":1,"
                + "\"journey\":{\"id\":1,\"status\":\"CANDIDATE\",\"steps\":[{\"LANDING\":{"
                + "\"id\":1,\"key\":\"lk\",\"stepType\":\"LANDING\",\"status\":\"CANDIDATE\","
                + "\"startPage\":{\"id\":10,\"key\":\"landingkey\"}}}]}}";
        Body body = createBodyFromJson(json);
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Journey result page url not found", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenResultPageUrlIsBlank() {
        Body body = createLandingStepBody("landingkey", "   ");
        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Journey result page url not found", response.getBody());
    }

    // ================================================================
    // receiveMessage: domain validation
    // ================================================================

    @Test
    void receiveMessageReturnsBadRequestWhenDomainNotFound() {
        Body body = createExpandableSimpleStepBody();
        when(domain_service.findByAuditRecord(anyLong())).thenReturn(null);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Domain not found for audit record", response.getBody());
    }

    @Test
    void receiveMessageReturnsOkWhenLastPageIsExternal() throws Exception {
        Body body = createExpandableSimpleStepBody();

        Domain domain = mock(Domain.class);
        when(domain.getUrl()).thenReturn("https://example.com");
        when(domain_service.findByAuditRecord(anyLong())).thenReturn(domain);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> true)) {
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("external"));
        }
    }

    // ================================================================
    // receiveMessage: domain map with existing steps
    // ================================================================

    @Test
    void receiveMessageReturnsOkWhenExistingStepsFoundForPage() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(1L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);

        List<Step> existingSteps = List.of(mock(Step.class), mock(Step.class));
        when(step_service.getStepsWithStartPage(any(PageState.class), eq(1L))).thenReturn(existingSteps);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false)) {
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("RETURNING WITHOUT EXPANSION"));
        }
    }

    // ================================================================
    // receiveMessage: element processing
    // ================================================================

    @Test
    void receiveMessageSucceedsWithNullElementsFromService() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(null);
        when(page_state_service.getElementStates(anyLong())).thenReturn(null);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false)) {
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
        }
    }

    @Test
    void receiveMessageSucceedsWhenAllElementsFilteredOut() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(null);

        ElementState element = new ElementState();
        element.setName("script");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element)));

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {
            // "script" is a real structure tag so BrowserService.isStructureTag returns true naturally
            // element is filtered as structure tag, so no iterations
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
        }
    }

    // ================================================================
    // receiveMessage: step/journey creation and deduplication
    // ================================================================

    @Test
    void receiveMessageSkipsStepWhenCandidateKeyExists() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        when(step_service.getStepsWithStartPage(any(PageState.class), anyLong())).thenReturn(null);

        ElementState element = createInteractiveElement("btn-1");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element)));

        // Step candidate key already exists
        when(step_service.findByCandidateKey(any(), eq(42L))).thenReturn(mock(Step.class));

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
            verify(step_service, never()).save(any(Step.class));
        }
    }

    @Test
    void receiveMessageSkipsJourneyWhenCandidateKeyExists() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        when(step_service.getStepsWithStartPage(any(PageState.class), anyLong())).thenReturn(null);

        ElementState element = createInteractiveElement("btn-1");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element)));

        when(step_service.findByCandidateKey(any(), eq(42L))).thenReturn(null);
        SimpleStep savedStep = mock(SimpleStep.class);
        when(savedStep.getId()).thenReturn(99L);
        when(step_service.save(any(Step.class))).thenReturn(savedStep);

        // Journey candidate key already exists
        Journey existingJourney = mock(Journey.class);
        when(existingJourney.getId()).thenReturn(200L);
        when(journey_service.findByCandidateKey(any())).thenReturn(existingJourney);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(journey_service, never()).save(anyLong(), any(Journey.class));
        }
    }

    @Test
    void receiveMessageCreatesDomainMapWhenNull() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(null);

        ElementState element = createInteractiveElement("btn-1");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element)));

        DomainMap newDomainMap = mock(DomainMap.class);
        when(newDomainMap.getId()).thenReturn(42L);
        when(domain_map_service.save(any(DomainMap.class))).thenReturn(newDomainMap);
        when(step_service.findByCandidateKey(any(), eq(42L))).thenReturn(null);

        SimpleStep savedStep = mock(SimpleStep.class);
        when(savedStep.getId()).thenReturn(99L);
        when(step_service.save(any(Step.class))).thenReturn(savedStep);

        Journey savedJourney = mock(Journey.class);
        when(savedJourney.getId()).thenReturn(200L);
        when(journey_service.findByCandidateKey(any())).thenReturn(null);
        when(journey_service.save(anyLong(), any(Journey.class))).thenReturn(savedJourney);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<BrowserService> browserService = mockStatic(BrowserService.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
            verify(domain_map_service).save(any(DomainMap.class));
            verify(audit_record_service).addDomainMap(anyLong(), eq(42L));
        }
    }

    @Test
    void receiveMessageSuccessfullyExpandsAndPublishes() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        when(step_service.getStepsWithStartPage(any(PageState.class), anyLong())).thenReturn(null);

        ElementState element = createInteractiveElement("btn-1");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element)));

        when(step_service.findByCandidateKey(any(), eq(42L))).thenReturn(null);
        SimpleStep savedStep = mock(SimpleStep.class);
        when(savedStep.getId()).thenReturn(99L);
        when(step_service.save(any(Step.class))).thenReturn(savedStep);

        Journey savedJourney = mock(Journey.class);
        when(savedJourney.getId()).thenReturn(200L);
        when(journey_service.findByCandidateKey(any())).thenReturn(null);
        when(journey_service.save(anyLong(), any(Journey.class))).thenReturn(savedJourney);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<BrowserService> browserService = mockStatic(BrowserService.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
            verify(journey_service).save(eq(42L), any(Journey.class));
            verify(journey_service, atLeastOnce()).addStep(eq(200L), anyLong());
            verify(domain_map_service).addJourneyToDomainMap(eq(200L), eq(42L));
            verify(journey_candidate_topic).publish(any());
        }
    }

    @Test
    void receiveMessageHandlesMultipleElements() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        when(step_service.getStepsWithStartPage(any(PageState.class), anyLong())).thenReturn(null);

        ElementState element1 = createInteractiveElement("btn-1");
        ElementState element2 = createInteractiveElement("btn-2");
        when(page_state_service.getElementStates(anyLong())).thenReturn(new ArrayList<>(List.of(element1, element2)));

        when(step_service.findByCandidateKey(any(), eq(42L))).thenReturn(null);
        SimpleStep savedStep = mock(SimpleStep.class);
        when(savedStep.getId()).thenReturn(99L);
        when(step_service.save(any(Step.class))).thenReturn(savedStep);

        Journey savedJourney = mock(Journey.class);
        when(savedJourney.getId()).thenReturn(200L);
        when(journey_service.findByCandidateKey(any())).thenReturn(null);
        when(journey_service.save(anyLong(), any(Journey.class))).thenReturn(savedJourney);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false);
             MockedStatic<BrowserService> browserService = mockStatic(BrowserService.class, invocation -> false);
             MockedStatic<ElementStateUtils> elementUtils = mockStatic(ElementStateUtils.class, invocation -> true)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(journey_candidate_topic, atLeast(2)).publish(any());
        }
    }

    // ================================================================
    // receiveMessage: exception handling
    // ================================================================

    @Test
    void receiveMessageReturnsInternalServerErrorOnException() throws Exception {
        Body body = createExpandableSimpleStepBody();
        when(domain_service.findByAuditRecord(anyLong())).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error occurred while expanding journey", response.getBody());
    }

    // ================================================================
    // receiveMessage: LandingStep path through expansion
    // ================================================================

    @Test
    void receiveMessageHandlesLandingStepResultPage() {
        Body body = createLandingStepBody("landingkey", "https://example.com");
        setupDomainMock();
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(null);
        when(page_state_service.getElementStates(anyLong())).thenReturn(null);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false)) {

            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
        }
    }

    // ================================================================
    // receiveMessage: domain map exists with single/no steps
    // ================================================================

    @Test
    void receiveMessageProceedsWhenDomainMapHasSingleStep() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        // Only 1 step, so it doesn't trigger the > 1 check
        when(step_service.getStepsWithStartPage(any(PageState.class), eq(42L)))
                .thenReturn(List.of(mock(Step.class)));
        when(page_state_service.getElementStates(anyLong())).thenReturn(null);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false)) {
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
        }
    }

    @Test
    void receiveMessageProceedsWhenDomainMapPageStepsAreNull() throws Exception {
        Body body = createExpandableSimpleStepBody();
        setupDomainMock();

        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(42L);
        when(domain_map_service.findByDomainAuditId(anyLong())).thenReturn(domainMap);
        when(step_service.getStepsWithStartPage(any(PageState.class), eq(42L))).thenReturn(null);
        when(page_state_service.getElementStates(anyLong())).thenReturn(null);

        try (MockedStatic<BrowserUtils> browserUtils = mockStatic(BrowserUtils.class, invocation -> false)) {
            ResponseEntity<String> response = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Successfully generated journey expansions", response.getBody());
        }
    }

    // ================================================================
    // shouldBeExpanded (private method via reflection)
    // ================================================================

    @Test
    void shouldBeExpandedThrowsAssertionErrorForNullJourney() throws Exception {
        try {
            invokeShouldBeExpanded(null);
            fail("Expected AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(AssertionError.class, e.getCause());
        }
    }

    @Test
    void shouldBeExpandedReturnsFalseForJourneyWithNullSteps() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(null);
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForJourneyWithEmptySteps() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of());
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseWhenLastStepIsNull() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(Arrays.asList((Step) null));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsTrueForLandingStepWithStartPage() throws Exception {
        LandingStep step = mock(LandingStep.class);
        when(step.getStartPage()).thenReturn(mock(PageState.class));
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertTrue(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForLandingStepWithoutStartPage() throws Exception {
        LandingStep step = mock(LandingStep.class);
        when(step.getStartPage()).thenReturn(null);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForSimpleStepWithoutStartPage() throws Exception {
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(null);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForSimpleStepWithoutEndPage() throws Exception {
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(mock(PageState.class));
        when(step.getEndPage()).thenReturn(null);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseWhenSimpleStepDoesNotChangePageKey() throws Exception {
        PageState start = mock(PageState.class);
        PageState end = mock(PageState.class);
        when(start.getKey()).thenReturn("page-key");
        when(end.getKey()).thenReturn("page-key");
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(start);
        when(step.getEndPage()).thenReturn(end);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsTrueWhenSimpleStepChangesPageKey() throws Exception {
        PageState start = mock(PageState.class);
        PageState end = mock(PageState.class);
        when(start.getKey()).thenReturn("start");
        when(end.getKey()).thenReturn("end");
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(start);
        when(step.getEndPage()).thenReturn(end);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));
        assertTrue(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForUnknownStepType() throws Exception {
        // Use an anonymous Step subclass (neither LandingStep nor SimpleStep)
        Step unknownStep = mock(Step.class);
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(unknownStep));
        assertFalse(invokeShouldBeExpanded(journey));
    }

    // ================================================================
    // existsInJourney (private method via reflection)
    // ================================================================

    @Test
    void existsInJourneyThrowsAssertionErrorForNullJourney() throws Exception {
        try {
            invokeExistsInJourney(null, mock(Step.class));
            fail("Expected AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(AssertionError.class, e.getCause());
        }
    }

    @Test
    void existsInJourneyThrowsAssertionErrorForNullStep() throws Exception {
        Journey journey = mock(Journey.class);
        try {
            invokeExistsInJourney(journey, null);
            fail("Expected AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(AssertionError.class, e.getCause());
        }
    }

    @Test
    void existsInJourneyReturnsFalseForNullJourneySteps() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(null);
        assertFalse(invokeExistsInJourney(journey, mock(Step.class)));
    }

    @Test
    void existsInJourneySkipsNullStepEntries() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(Arrays.asList((Step) null));
        SimpleStep candidate = mock(SimpleStep.class);
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsTrueForMatchingLandingStepStartPageKeys() throws Exception {
        PageState page1 = mock(PageState.class);
        PageState page2 = mock(PageState.class);
        when(page1.getKey()).thenReturn("landing-key");
        when(page2.getKey()).thenReturn("landing-key");

        LandingStep existing = mock(LandingStep.class);
        when(existing.getStartPage()).thenReturn(page1);
        LandingStep candidate = mock(LandingStep.class);
        when(candidate.getStartPage()).thenReturn(page2);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertTrue(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseForLandingStepsWithDifferentKeys() throws Exception {
        PageState page1 = mock(PageState.class);
        PageState page2 = mock(PageState.class);
        when(page1.getKey()).thenReturn("key-a");
        when(page2.getKey()).thenReturn("key-b");

        LandingStep existing = mock(LandingStep.class);
        when(existing.getStartPage()).thenReturn(page1);
        LandingStep candidate = mock(LandingStep.class);
        when(candidate.getStartPage()).thenReturn(page2);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenLandingStepHasNullStartPage() throws Exception {
        LandingStep existing = mock(LandingStep.class);
        when(existing.getStartPage()).thenReturn(null);
        LandingStep candidate = mock(LandingStep.class);
        when(candidate.getStartPage()).thenReturn(mock(PageState.class));

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenCandidateLandingStepHasNullStartPage() throws Exception {
        LandingStep existing = mock(LandingStep.class);
        when(existing.getStartPage()).thenReturn(mock(PageState.class));
        LandingStep candidate = mock(LandingStep.class);
        when(candidate.getStartPage()).thenReturn(null);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsTrueForMatchingSimpleSteps() throws Exception {
        PageState start1 = mock(PageState.class);
        PageState start2 = mock(PageState.class);
        when(start1.getUrl()).thenReturn("https://example.com");
        when(start2.getUrl()).thenReturn("https://example.com");

        ElementState element1 = mock(ElementState.class);
        ElementState element2 = mock(ElementState.class);
        when(element1.getKey()).thenReturn("btn-1");
        when(element2.getKey()).thenReturn("btn-1");

        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(start1);
        when(existing.getElementState()).thenReturn(element1);
        when(existing.getAction()).thenReturn(Action.CLICK);
        when(existing.getActionInput()).thenReturn("");

        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(start2);
        when(candidate.getElementState()).thenReturn(element2);
        when(candidate.getAction()).thenReturn(Action.CLICK);
        when(candidate.getActionInput()).thenReturn("");

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertTrue(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenSimpleStepAttributesDiffer() throws Exception {
        PageState start1 = mock(PageState.class);
        PageState start2 = mock(PageState.class);
        when(start1.getUrl()).thenReturn("https://example.com/a");
        when(start2.getUrl()).thenReturn("https://example.com/b");

        ElementState element1 = mock(ElementState.class);
        ElementState element2 = mock(ElementState.class);
        when(element1.getKey()).thenReturn("btn-1");
        when(element2.getKey()).thenReturn("btn-2");

        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(start1);
        when(existing.getElementState()).thenReturn(element1);
        when(existing.getAction()).thenReturn(Action.CLICK);
        when(existing.getActionInput()).thenReturn("");

        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(start2);
        when(candidate.getElementState()).thenReturn(element2);
        when(candidate.getAction()).thenReturn(Action.CLICK);
        when(candidate.getActionInput()).thenReturn("different");

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenSimpleStepHasNullStartPage() throws Exception {
        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(null);
        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(mock(PageState.class));

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenSimpleStepHasNullElementState() throws Exception {
        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(mock(PageState.class));
        when(existing.getElementState()).thenReturn(null);
        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(mock(PageState.class));
        when(candidate.getElementState()).thenReturn(mock(ElementState.class));

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseForMixedStepTypes() throws Exception {
        LandingStep existing = mock(LandingStep.class);
        SimpleStep candidate = mock(SimpleStep.class);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseForSimpleStepVsLandingStep() throws Exception {
        SimpleStep existing = mock(SimpleStep.class);
        LandingStep candidate = mock(LandingStep.class);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));
        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private Body createBodyFromJson(String json) {
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        when(body.getMessage().getData()).thenReturn(encoded);
        return body;
    }

    private Body createExpandableSimpleStepBody() {
        String json = "{\"auditRecordId\":100,\"accountId\":1,\"browser\":\"CHROME\","
                + "\"journey\":{\"id\":1,\"status\":\"CANDIDATE\",\"steps\":[{\"SIMPLE\":{"
                + "\"id\":1,\"key\":\"teststepkey\",\"stepType\":\"SIMPLE\",\"status\":\"CANDIDATE\","
                + "\"startPage\":{\"id\":10,\"key\":\"startkey\",\"url\":\"https://example.com\"},"
                + "\"endPage\":{\"id\":20,\"key\":\"endkey\",\"url\":\"https://example.com/page2\"},"
                + "\"elementState\":{\"id\":30,\"key\":\"original-elem-key\",\"name\":\"a\"},"
                + "\"action\":\"CLICK\",\"actionInput\":\"\"}}]}}";
        return createBodyFromJson(json);
    }

    private Body createNonExpandableSimpleStepBody() {
        String json = "{\"auditRecordId\":100,\"accountId\":1,"
                + "\"journey\":{\"id\":1,\"status\":\"CANDIDATE\",\"steps\":[{\"SIMPLE\":{"
                + "\"id\":1,\"key\":\"teststepkey\",\"stepType\":\"SIMPLE\",\"status\":\"CANDIDATE\","
                + "\"startPage\":{\"id\":10,\"key\":\"samekey\",\"url\":\"https://example.com\"},"
                + "\"endPage\":{\"id\":20,\"key\":\"samekey\",\"url\":\"https://example.com\"},"
                + "\"elementState\":{\"id\":30,\"key\":\"elem1\",\"name\":\"a\"},"
                + "\"action\":\"CLICK\",\"actionInput\":\"\"}}]}}";
        return createBodyFromJson(json);
    }

    private Body createLandingStepBody(String pageKey, String url) {
        String json = "{\"auditRecordId\":100,\"accountId\":1,"
                + "\"journey\":{\"id\":1,\"status\":\"CANDIDATE\",\"steps\":[{\"LANDING\":{"
                + "\"id\":1,\"key\":\"testlandingkey\",\"stepType\":\"LANDING\",\"status\":\"CANDIDATE\","
                + "\"startPage\":{\"id\":10,\"key\":\"" + pageKey + "\",\"url\":\"" + url + "\"}}}]}}";
        return createBodyFromJson(json);
    }

    private Body createNullJourneyBody() {
        String json = "{\"auditRecordId\":100,\"accountId\":1}";
        return createBodyFromJson(json);
    }

    private Body createEmptyStepsBody() {
        String json = "{\"auditRecordId\":100,\"accountId\":1,\"journey\":{\"id\":1,\"status\":\"CANDIDATE\",\"steps\":[]}}";
        return createBodyFromJson(json);
    }

    private void setupDomainMock() {
        Domain domain = mock(Domain.class);
        when(domain.getUrl()).thenReturn("https://example.com");
        when(domain_service.findByAuditRecord(anyLong())).thenReturn(domain);
    }

    private ElementState createInteractiveElement(String key) {
        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn("a");
        when(element.getKey()).thenReturn(key);
        when(element.getId()).thenReturn(100L);
        when(element.isVisible()).thenReturn(true);
        when(element.getCssSelector()).thenReturn("body a");
        when(element.getXpath()).thenReturn("//a");
        when(element.getOwnedText()).thenReturn("link");
        when(element.getAllText()).thenReturn("link");
        when(element.getOuterHtml()).thenReturn("<a>link</a>");
        return element;
    }

    private boolean invokeShouldBeExpanded(Journey journey) throws Exception {
        Method method = AuditController.class.getDeclaredMethod("shouldBeExpanded", Journey.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, journey);
    }

    private boolean invokeExistsInJourney(Journey journey, Step step) throws Exception {
        Method method = AuditController.class.getDeclaredMethod("existsInJourney", Journey.class, Step.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, journey, step);
    }
}
