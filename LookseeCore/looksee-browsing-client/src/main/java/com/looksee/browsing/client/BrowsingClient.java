package com.looksee.browsing.client;

import com.looksee.browsing.generated.ApiClient;
import com.looksee.browsing.generated.ApiException;
import com.looksee.browsing.generated.api.AlertsApi;
import com.looksee.browsing.generated.api.CaptureApi;
import com.looksee.browsing.generated.api.DomApi;
import com.looksee.browsing.generated.api.ElementsApi;
import com.looksee.browsing.generated.api.MouseApi;
import com.looksee.browsing.generated.api.NavigationApi;
import com.looksee.browsing.generated.api.ScreenshotsApi;
import com.looksee.browsing.generated.api.ScrollingApi;
import com.looksee.browsing.generated.api.SessionsApi;
import com.looksee.browsing.generated.api.TouchApi;
import com.looksee.browsing.generated.model.AlertChoice;
import com.looksee.browsing.generated.model.AlertRespondRequest;
import com.looksee.browsing.generated.model.AlertState;
import com.looksee.browsing.generated.model.CaptureRequest;
import com.looksee.browsing.generated.model.CaptureResponse;
import com.looksee.browsing.generated.model.CreateSessionRequest;
import com.looksee.browsing.generated.model.DomRemovePreset;
import com.looksee.browsing.generated.model.DomRemoveRequest;
import com.looksee.browsing.generated.model.ElementAction;
import com.looksee.browsing.generated.model.ElementActionRequest;
import com.looksee.browsing.generated.model.ElementScreenshotRequest;
import com.looksee.browsing.generated.model.ElementState;
import com.looksee.browsing.generated.model.ElementTouchRequest;
import com.looksee.browsing.generated.model.FindElementRequest;
import com.looksee.browsing.generated.model.MouseMoveMode;
import com.looksee.browsing.generated.model.MouseMoveRequest;
import com.looksee.browsing.generated.model.NavigateRequest;
import com.looksee.browsing.generated.model.PageStatus;
import com.looksee.browsing.generated.model.ScreenshotEncoding;
import com.looksee.browsing.generated.model.ScreenshotRequest;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.ScrollMode;
import com.looksee.browsing.generated.model.ScrollOffset;
import com.looksee.browsing.generated.model.ScrollRequest;
import com.looksee.browsing.generated.model.Session;
import com.looksee.browsing.generated.model.SessionState;
import com.looksee.browsing.generated.model.TouchAction;
import com.looksee.browsing.generated.model.ViewportState;
import java.math.BigDecimal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hand-written facade over the generated browser-service client. This is the
 * single entry point looksee-core uses — keeping OpenAPI drift contained to
 * this class instead of rippling through every consumer.
 *
 * <p>Exposes the generated model types directly for now. Wrapping them in
 * hand-written POJOs is a later simplification if the OpenAPI surface churns.
 *
 * <p><b>Instrumentation.</b> When a {@link MeterRegistry} is supplied, every
 * public facade method emits a Micrometer {@link Timer} named
 * {@code browser_service_calls} with tags {@code operation=<method-name>}
 * and {@code outcome=success|failure}. Consumers add a {@code consumer}
 * common tag in their own config so dashboards can filter per-caller without
 * the facade needing to know who's calling. See
 * {@code browser-service/phase-4-consumer-cutover.md} §Observability prereqs
 * for the full metric contract and PromQL examples.
 *
 * <p>Every failure path also logs a structured warn line before rethrowing as
 * {@link BrowsingClientException} so Sentry / Cloud Logging pick it up.
 */
public class BrowsingClient {

    private static final Logger log = LoggerFactory.getLogger(BrowsingClient.class);
    private static final String METRIC_NAME = "browser_service_calls";

    private final SessionsApi sessionsApi;
    private final NavigationApi navigationApi;
    private final ScreenshotsApi screenshotsApi;
    private final ScrollingApi scrollingApi;
    private final CaptureApi captureApi;
    private final ElementsApi elementsApi;
    private final TouchApi touchApi;
    private final DomApi domApi;
    private final MouseApi mouseApi;
    private final AlertsApi alertsApi;
    private final MeterRegistry meterRegistry; // may be null — treated as no-op

    public BrowsingClient(BrowsingClientConfig config) {
        this(config, null);
    }

    public BrowsingClient(BrowsingClientConfig config, MeterRegistry meterRegistry) {
        Objects.requireNonNull(config, "config");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(config.getServiceUrl());
        apiClient.setConnectTimeout(config.getConnectTimeout());
        apiClient.setReadTimeout(config.getReadTimeout());
        this.sessionsApi = new SessionsApi(apiClient);
        this.navigationApi = new NavigationApi(apiClient);
        this.screenshotsApi = new ScreenshotsApi(apiClient);
        this.scrollingApi = new ScrollingApi(apiClient);
        this.captureApi = new CaptureApi(apiClient);
        this.elementsApi = new ElementsApi(apiClient);
        this.touchApi = new TouchApi(apiClient);
        this.domApi = new DomApi(apiClient);
        this.mouseApi = new MouseApi(apiClient);
        this.alertsApi = new AlertsApi(apiClient);
        this.meterRegistry = meterRegistry;
    }

    /** Package-private test constructor — no meter registry, test-only *Api mocks. */
    BrowsingClient(SessionsApi sessionsApi,
                   NavigationApi navigationApi,
                   ScreenshotsApi screenshotsApi,
                   ScrollingApi scrollingApi,
                   CaptureApi captureApi) {
        this(sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi,
             null, null, null, null, null, null);
    }

    /** Package-private test constructor — with meter registry, legacy 5-api form. */
    BrowsingClient(SessionsApi sessionsApi,
                   NavigationApi navigationApi,
                   ScreenshotsApi screenshotsApi,
                   ScrollingApi scrollingApi,
                   CaptureApi captureApi,
                   MeterRegistry meterRegistry) {
        this(sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi,
             null, null, null, null, null, meterRegistry);
    }

    /** Package-private test constructor — full 10-api form for phase-3b facade tests. */
    BrowsingClient(SessionsApi sessionsApi,
                   NavigationApi navigationApi,
                   ScreenshotsApi screenshotsApi,
                   ScrollingApi scrollingApi,
                   CaptureApi captureApi,
                   ElementsApi elementsApi,
                   TouchApi touchApi,
                   DomApi domApi,
                   MouseApi mouseApi,
                   AlertsApi alertsApi,
                   MeterRegistry meterRegistry) {
        this.sessionsApi = sessionsApi;
        this.navigationApi = navigationApi;
        this.screenshotsApi = screenshotsApi;
        this.scrollingApi = scrollingApi;
        this.captureApi = captureApi;
        this.elementsApi = elementsApi;
        this.touchApi = touchApi;
        this.domApi = domApi;
        this.mouseApi = mouseApi;
        this.alertsApi = alertsApi;
        this.meterRegistry = meterRegistry;
    }

    // --- Session lifecycle -------------------------------------------------

    public Session createSession(com.looksee.browser.enums.BrowserType type,
                                 com.looksee.browser.enums.BrowserEnvironment env) {
        return recordCall("createSession", "", () -> {
            CreateSessionRequest req = new CreateSessionRequest()
                .browser(toGenerated(type))
                .environment(toGenerated(env));
            return sessionsApi.createSession(req);
        });
    }

    public SessionState getSession(String id) {
        return recordCall("getSession", id, () -> sessionsApi.getSession(id));
    }

    public void deleteSession(String id) {
        recordCall("deleteSession", id, () -> { sessionsApi.deleteSession(id); return null; });
    }

    // --- Navigation + state ------------------------------------------------

    public void navigate(String id, String url) {
        recordCall("navigate", id + " -> " + url, () -> {
            navigationApi.navigate(id, new NavigateRequest().url(java.net.URI.create(url)));
            return null;
        });
    }

    public PageStatus getStatus(String id) {
        return recordCall("getStatus", id, () -> navigationApi.getPageStatus(id));
    }

    public ViewportState getViewport(String id) {
        return recordCall("getViewport", id, () -> scrollingApi.getViewport(id));
    }

    public String getSource(String id) {
        return recordCall("getSource", id, () -> navigationApi.getSource(id));
    }

    // --- Screenshots -------------------------------------------------------

    public byte[] screenshot(String id, ScreenshotStrategy strategy) {
        return recordCall("screenshot", id, () -> {
            ScreenshotRequest req = new ScreenshotRequest()
                .strategy(strategy)
                .encoding(ScreenshotEncoding.BINARY);
            File f = screenshotsApi.captureScreenshot(id, req);
            try {
                return Files.readAllBytes(f.toPath());
            } catch (IOException e) {
                throw new BrowsingClientException("read screenshot bytes failed: " + id, e);
            }
        });
    }

    // --- Capture (one-shot) ------------------------------------------------

    public CaptureResponse capture(CaptureRequest req) {
        return recordCall("capture", "", () -> captureApi.capture(req));
    }

    public byte[] getCaptureScreenshotBytes(String captureId) {
        return recordCall("getCaptureScreenshotBytes", captureId, () -> {
            File f = captureApi.getCaptureScreenshot(captureId);
            try {
                return Files.readAllBytes(f.toPath());
            } catch (IOException e) {
                throw new BrowsingClientException("read capture bytes failed: " + captureId, e);
            }
        });
    }

    // --- Element lookup + action (phase 3b) -------------------------------

    public ElementState findElement(String sessionId, String xpath) {
        return recordCall("findElement", sessionId + " " + xpath, () ->
            elementsApi.findElement(sessionId, new FindElementRequest().xpath(xpath)));
    }

    public void performElementAction(String sessionId, String elementHandle,
                                     ElementAction action, String input) {
        recordCall("performElementAction", sessionId, () -> {
            elementsApi.performElementAction(sessionId,
                new ElementActionRequest()
                    .elementHandle(elementHandle)
                    .action(action)
                    .input(input));
            return null;
        });
    }

    public void performElementTouch(String sessionId, String elementHandle,
                                    TouchAction action, String input) {
        recordCall("performElementTouch", sessionId, () -> {
            touchApi.performElementTouch(sessionId,
                new ElementTouchRequest()
                    .elementHandle(elementHandle)
                    .action(action)
                    .input(input));
            return null;
        });
    }

    public byte[] captureElementScreenshot(String sessionId, String elementHandle) {
        return recordCall("captureElementScreenshot", sessionId, () -> {
            File f = screenshotsApi.captureElementScreenshot(sessionId,
                new ElementScreenshotRequest()
                    .elementHandle(elementHandle)
                    .encoding(ScreenshotEncoding.BINARY));
            try {
                return Files.readAllBytes(f.toPath());
            } catch (IOException e) {
                throw new BrowsingClientException("read element screenshot bytes failed: " + sessionId, e);
            }
        });
    }

    // --- Scroll ops (phase 3b) --------------------------------------------

    public ScrollOffset scrollToTop(String sessionId) {
        return recordCall("scrollToTop", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest().mode(ScrollMode.TO_TOP)));
    }

    public ScrollOffset scrollToBottom(String sessionId) {
        return recordCall("scrollToBottom", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest().mode(ScrollMode.TO_BOTTOM)));
    }

    public ScrollOffset scrollToElement(String sessionId, String elementHandle, String xpathHint) {
        return recordCall("scrollToElement", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest()
                .mode(ScrollMode.TO_ELEMENT)
                .elementHandle(elementHandle)
                .xpath(xpathHint)));
    }

    public ScrollOffset scrollToElementCentered(String sessionId, String elementHandle) {
        return recordCall("scrollToElementCentered", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest()
                .mode(ScrollMode.TO_ELEMENT_CENTERED)
                .elementHandle(elementHandle)));
    }

    public ScrollOffset scrollDownPercent(String sessionId, double percent) {
        return recordCall("scrollDownPercent", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest()
                .mode(ScrollMode.DOWN_PERCENT)
                .percent(BigDecimal.valueOf(percent))));
    }

    public ScrollOffset scrollDownFull(String sessionId) {
        return recordCall("scrollDownFull", sessionId, () ->
            scrollingApi.scroll(sessionId, new ScrollRequest().mode(ScrollMode.DOWN_FULL)));
    }

    // --- DOM removal (phase 3b) -------------------------------------------

    public void removeDomElement(String sessionId, DomRemovePreset preset, String valueOrNull) {
        recordCall("removeDomElement", sessionId, () -> {
            domApi.removeDomElement(sessionId,
                new DomRemoveRequest().preset(preset).value(valueOrNull));
            return null;
        });
    }

    // --- Mouse (phase 3b) -------------------------------------------------

    public void moveMouseOutOfFrame(String sessionId) {
        recordCall("moveMouseOutOfFrame", sessionId, () -> {
            mouseApi.moveMouse(sessionId,
                new MouseMoveRequest().mode(MouseMoveMode.OUT_OF_FRAME));
            return null;
        });
    }

    public void moveMouseToNonInteractive(String sessionId, int x, int y) {
        recordCall("moveMouseToNonInteractive", sessionId, () -> {
            mouseApi.moveMouse(sessionId,
                new MouseMoveRequest()
                    .mode(MouseMoveMode.TO_NON_INTERACTIVE)
                    .x(x)
                    .y(y));
            return null;
        });
    }

    // --- Alert (phase 3b) -------------------------------------------------

    public AlertState getAlert(String sessionId) {
        return recordCall("getAlert", sessionId, () -> alertsApi.getAlert(sessionId));
    }

    public void respondToAlert(String sessionId, AlertChoice choice, String inputOrNull) {
        recordCall("respondToAlert", sessionId, () -> {
            alertsApi.respondToAlert(sessionId,
                new AlertRespondRequest().choice(choice).input(inputOrNull));
            return null;
        });
    }

    // --- Instrumentation helper -------------------------------------------

    @FunctionalInterface
    private interface ApiCall<T> {
        T invoke() throws ApiException;
    }

    private <T> T recordCall(String operation, String contextForErrorMessage, ApiCall<T> call) {
        long start = System.nanoTime();
        // Default to failure; flipped to success only once call.invoke() returns
        // normally. Ensures unexpected runtime errors (e.g. IllegalArgumentException
        // from URI.create) don't produce a false-success metric entry.
        String outcome = "failure";
        try {
            T result = call.invoke();
            outcome = "success";
            return result;
        } catch (ApiException e) {
            String ctx = contextForErrorMessage.isEmpty() ? "" : (": " + contextForErrorMessage);
            log.warn("BrowsingClient.{}{} failed (status={}): {}",
                operation, ctx, e.getCode(), e.getMessage());
            throw new BrowsingClientException(operation + " failed" + ctx, e);
        } catch (BrowsingClientException e) {
            // IO failure inside a nested screenshot read — already has context.
            log.warn("BrowsingClient.{} failed: {}", operation, e.getMessage());
            throw e;
        } finally {
            if (meterRegistry != null) {
                Timer.builder(METRIC_NAME)
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            }
        }
    }

    // --- Enum translation --------------------------------------------------

    private static com.looksee.browsing.generated.model.BrowserType
            toGenerated(com.looksee.browser.enums.BrowserType type) {
        // Both enums share lowercase wire values; see openapi.yaml and
        // com.looksee.browser.enums.BrowserType.
        return com.looksee.browsing.generated.model.BrowserType.fromValue(type.toString());
    }

    private static com.looksee.browsing.generated.model.BrowserEnvironment
            toGenerated(com.looksee.browser.enums.BrowserEnvironment env) {
        return com.looksee.browsing.generated.model.BrowserEnvironment.fromValue(env.toString());
    }
}
