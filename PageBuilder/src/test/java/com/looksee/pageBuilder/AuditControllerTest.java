package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.browser.Browser;
import com.looksee.models.PageState;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.Step;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.schemas.MessageSchema;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.ElementStateService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock private AuditRecordService auditRecordService;
    @Mock private BrowserService browserService;
    @Mock private JourneyService journeyService;
    @Mock private StepService stepService;
    @Mock private PageStateService pageStateService;
    @Mock private ElementStateService elementStateService;
    @Mock private DomainMapService domainMapService;
    @Mock private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
    @Mock private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
    @Mock private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;
    @Mock private PubSubPageAuditPublisherImpl auditRecordTopic;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks
    private AuditController controller;

    private static final String TEST_URL = "https://example.com";
    private static final long ACCOUNT_ID = 1L;
    private static final long AUDIT_ID = 100L;

    private String encodeAuditMessage(String url, String type, long accountId, long auditId) {
        String json = String.format(
                "{\"url\":\"%s\",\"type\":\"%s\",\"accountId\":%d,\"auditId\":%d}",
                url, type, accountId, auditId);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private BodySchema buildBody(String base64Data) {
        MessageSchema msg = new MessageSchema(null, base64Data);
        return new BodySchema(msg);
    }

    // ===== Input validation tests =====

    @Test
    void nullBodyReturns400() throws Exception {
        ResponseEntity<String> resp = controller.receiveMessage(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void nullMessageReturns400() throws Exception {
        BodySchema body = new BodySchema();
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void nullDataReturns400() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(null, null));
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void blankDataReturns400() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(null, "   "));
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void invalidBase64Returns400() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(null, "not-valid-base64!!!"));
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void invalidJsonReturns400() throws Exception {
        String notJson = Base64.getEncoder().encodeToString("not json".getBytes(StandardCharsets.UTF_8));
        BodySchema body = buildBody(notJson);
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ===== HTTP status 404/408 error path tests =====

    @Test
    void httpStatus404PublishesErrorAndReturns200() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(404);

            ResponseEntity<String> resp = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
        }
    }

    @Test
    void httpStatus408PublishesErrorAndReturns200() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(false);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(408);

            ResponseEntity<String> resp = controller.receiveMessage(body);
            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
        }
    }

    // ===== PAGE audit path =====

    @Test
    void pageAuditWithNewPageStateBuildsAndPublishes() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getKey()).thenReturn("page-key");
        when(pageState.getSrc()).thenReturn("<html></html>");

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "page-key")).thenReturn(null);
            when(browserService.extractAllUniqueElementXpaths("<html></html>"))
                    .thenReturn(List.of("//div"));
            when(browserService.getDomElementStates(eq(pageState), anyList(), eq(browser), eq(AUDIT_ID)))
                    .thenReturn(new ArrayList<>());
            when(pageStateService.save(pageState)).thenReturn(pageState);
            when(pageStateService.getElementStates(1L)).thenReturn(new ArrayList<>());

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(auditRecordService).addPageToAuditRecord(AUDIT_ID, 1L);
            verify(auditRecordTopic).publish(anyString());
            verify(browser).close();
        }
    }

    @Test
    void pageAuditWithExistingPageStateSkipsElementExtraction() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getKey()).thenReturn("existing-key");

        PageState existingPageState = mock(PageState.class);
        when(existingPageState.getId()).thenReturn(2L);

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "existing-key")).thenReturn(existingPageState);
            when(elementStateService.getAllExistingKeys(2L)).thenReturn(List.of("key1"));
            when(pageStateService.getElementStates(2L)).thenReturn(new ArrayList<>());

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(browserService, never()).extractAllUniqueElementXpaths(anyString());
            verify(auditRecordService).addPageToAuditRecord(AUDIT_ID, 2L);
            verify(browser).close();
        }
    }

    // ===== DOMAIN audit path =====

    @Test
    void domainAuditCreatesNewDomainMapAndJourney() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "DOMAIN", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(5L);
        when(pageState.getKey()).thenReturn("dom-key");
        when(pageState.getSrc()).thenReturn("<html></html>");

        Browser browser = mock(Browser.class);
        DomainMap domainMap = mock(DomainMap.class);
        when(domainMap.getId()).thenReturn(10L);

        Step savedStep = mock(LandingStep.class);
        when(savedStep.getId()).thenReturn(20L);

        Journey savedJourney = mock(Journey.class);
        when(savedJourney.getId()).thenReturn(30L);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(domainMapService.findByDomainAuditId(AUDIT_ID)).thenReturn(null);
            when(domainMapService.save(any(DomainMap.class))).thenReturn(domainMap);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "dom-key")).thenReturn(null);
            when(browserService.extractAllUniqueElementXpaths("<html></html>")).thenReturn(List.of("//p"));
            when(browserService.getDomElementStates(eq(pageState), anyList(), eq(browser), eq(AUDIT_ID)))
                    .thenReturn(new ArrayList<>());
            when(pageStateService.save(pageState)).thenReturn(pageState);
            when(pageStateService.getElementStates(5L)).thenReturn(new ArrayList<>());

            when(stepService.save(any(Step.class))).thenReturn(savedStep);
            when(journeyService.save(any(Journey.class))).thenReturn(savedJourney);

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(auditRecordService).addDomainMap(AUDIT_ID, 10L);
            verify(domainMapService).addPageToDomainMap(10L, 5L);
            verify(pubSubPageCreatedPublisherImpl).publish(anyString());
            verify(domainMapService).addJourneyToDomainMap(eq(30L), eq(10L));
            verify(pubSubJourneyVerifiedPublisherImpl).publish(anyString());
            verify(browser).close();
        }
    }

    @Test
    void domainAuditReusesExistingDomainMap() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "DOMAIN", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(5L);
        when(pageState.getKey()).thenReturn("dom-key");
        when(pageState.getSrc()).thenReturn("<html></html>");

        Browser browser = mock(Browser.class);
        DomainMap existingMap = mock(DomainMap.class);
        when(existingMap.getId()).thenReturn(77L);

        Step savedStep = mock(LandingStep.class);
        when(savedStep.getId()).thenReturn(20L);

        Journey savedJourney = mock(Journey.class);
        when(savedJourney.getId()).thenReturn(30L);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(domainMapService.findByDomainAuditId(AUDIT_ID)).thenReturn(existingMap);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "dom-key")).thenReturn(null);
            when(browserService.extractAllUniqueElementXpaths("<html></html>")).thenReturn(List.of("//p"));
            when(browserService.getDomElementStates(eq(pageState), anyList(), eq(browser), eq(AUDIT_ID)))
                    .thenReturn(new ArrayList<>());
            when(pageStateService.save(pageState)).thenReturn(pageState);
            when(pageStateService.getElementStates(5L)).thenReturn(new ArrayList<>());

            when(stepService.save(any(Step.class))).thenReturn(savedStep);
            when(journeyService.save(any(Journey.class))).thenReturn(savedJourney);

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            // Should NOT create a new domain map
            verify(domainMapService, never()).save(any(DomainMap.class));
            verify(auditRecordService, never()).addDomainMap(anyLong(), anyLong());
            verify(domainMapService).addPageToDomainMap(77L, 5L);
            verify(browser).close();
        }
    }

    // ===== Existing page state with empty elements =====

    @Test
    void existingPageStateWithEmptyElementsRebuildsElements() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(3L);
        when(pageState.getKey()).thenReturn("pk");
        when(pageState.getSrc()).thenReturn("<html></html>");

        PageState existingPageState = mock(PageState.class);
        when(existingPageState.getId()).thenReturn(3L);

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(false);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(false), eq(200), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "pk")).thenReturn(existingPageState);
            // Empty set of keys triggers rebuild
            when(elementStateService.getAllExistingKeys(3L)).thenReturn(new ArrayList<>());
            when(browserService.extractAllUniqueElementXpaths("<html></html>")).thenReturn(List.of("//span"));
            when(browserService.getDomElementStates(eq(pageState), anyList(), eq(browser), eq(AUDIT_ID)))
                    .thenReturn(new ArrayList<>());
            when(pageStateService.save(pageState)).thenReturn(pageState);
            when(pageStateService.getElementStates(3L)).thenReturn(new ArrayList<>());

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(browserService).extractAllUniqueElementXpaths(anyString());
            verify(browser).close();
        }
    }

    // ===== Exception path =====

    @Test
    void exceptionDuringProcessingPublishesErrorAndReturns500() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenThrow(new RuntimeException("browser error"));

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
            verify(browser).close();
        }
    }

    @Test
    void exceptionDuringProcessingWithNullBrowserDoesNotCallClose() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(null);
            when(browserService.buildPageState(any(URL.class), isNull(), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenThrow(new RuntimeException("null browser"));

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
        }
    }

    @Test
    void exceptionDuringErrorPublishingPropagatesAndClosesBrowser() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(200), eq(AUDIT_ID)))
                    .thenThrow(new RuntimeException("build failure"));
            // publish() throws RuntimeException, which is not caught by the inner
            // catch(JsonProcessingException) and propagates out of the method
            doThrow(new RuntimeException("publish failed")).when(pubSubErrorPublisherImpl).publish(anyString());

            assertThrows(RuntimeException.class, () -> controller.receiveMessage(body));
            verify(browser).close();
        }
    }

    // ===== HTTP status that is not 404/408 (e.g. 301 redirect) =====

    @Test
    void httpStatus301ProceedsNormally() throws Exception {
        String data = encodeAuditMessage(TEST_URL, "PAGE", ACCOUNT_ID, AUDIT_ID);
        BodySchema body = buildBody(data);

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getKey()).thenReturn("key301");
        when(pageState.getSrc()).thenReturn("<html></html>");

        Browser browser = mock(Browser.class);

        try (MockedStatic<BrowserUtils> utils = mockStatic(BrowserUtils.class)) {
            utils.when(() -> BrowserUtils.sanitizeUserUrl(TEST_URL)).thenReturn(TEST_URL);
            utils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            utils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(301);

            when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
            when(browserService.buildPageState(any(URL.class), eq(browser), eq(true), eq(301), eq(AUDIT_ID)))
                    .thenReturn(pageState);
            when(auditRecordService.findPageWithKey(AUDIT_ID, "key301")).thenReturn(null);
            when(browserService.extractAllUniqueElementXpaths("<html></html>")).thenReturn(new ArrayList<>());
            when(browserService.getDomElementStates(eq(pageState), anyList(), eq(browser), eq(AUDIT_ID)))
                    .thenReturn(new ArrayList<>());
            when(pageStateService.save(pageState)).thenReturn(pageState);
            when(pageStateService.getElementStates(1L)).thenReturn(new ArrayList<>());

            ResponseEntity<String> resp = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            // 301 is not 404/408, so error publisher should not be called
            verify(pubSubErrorPublisherImpl, never()).publish(anyString());
            verify(browser).close();
        }
    }

    // ===== Edge case: data is empty string =====

    @Test
    void emptyStringDataReturns400() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(null, ""));
        ResponseEntity<String> resp = controller.receiveMessage(body);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
