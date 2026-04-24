package com.looksee.browsing.client;

import com.looksee.browsing.generated.ApiClient;
import com.looksee.browsing.generated.ApiException;
import com.looksee.browsing.generated.api.CaptureApi;
import com.looksee.browsing.generated.api.NavigationApi;
import com.looksee.browsing.generated.api.ScreenshotsApi;
import com.looksee.browsing.generated.api.ScrollingApi;
import com.looksee.browsing.generated.api.SessionsApi;
import com.looksee.browsing.generated.model.CaptureRequest;
import com.looksee.browsing.generated.model.CaptureResponse;
import com.looksee.browsing.generated.model.CreateSessionRequest;
import com.looksee.browsing.generated.model.NavigateRequest;
import com.looksee.browsing.generated.model.PageStatus;
import com.looksee.browsing.generated.model.ScreenshotEncoding;
import com.looksee.browsing.generated.model.ScreenshotRequest;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.Session;
import com.looksee.browsing.generated.model.SessionState;
import com.looksee.browsing.generated.model.ViewportState;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Hand-written facade over the generated browser-service client. This is the
 * single entry point looksee-core uses — keeping OpenAPI drift contained to
 * this class instead of rippling through every consumer.
 *
 * <p>Exposes the generated model types directly for now. Wrapping them in
 * hand-written POJOs is a later simplification if the OpenAPI surface churns.
 */
public class BrowsingClient {

    private final SessionsApi sessionsApi;
    private final NavigationApi navigationApi;
    private final ScreenshotsApi screenshotsApi;
    private final ScrollingApi scrollingApi;
    private final CaptureApi captureApi;

    public BrowsingClient(BrowsingClientConfig config) {
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
    }

    /** Package-private test constructor: accepts mocked {@code *Api} instances. */
    BrowsingClient(SessionsApi sessionsApi,
                   NavigationApi navigationApi,
                   ScreenshotsApi screenshotsApi,
                   ScrollingApi scrollingApi,
                   CaptureApi captureApi) {
        this.sessionsApi = sessionsApi;
        this.navigationApi = navigationApi;
        this.screenshotsApi = screenshotsApi;
        this.scrollingApi = scrollingApi;
        this.captureApi = captureApi;
    }

    // --- Session lifecycle -------------------------------------------------

    public Session createSession(com.looksee.browser.enums.BrowserType type,
                                 com.looksee.browser.enums.BrowserEnvironment env) {
        CreateSessionRequest req = new CreateSessionRequest()
            .browser(toGenerated(type))
            .environment(toGenerated(env));
        try {
            return sessionsApi.createSession(req);
        } catch (ApiException e) {
            throw new BrowsingClientException("createSession failed", e);
        }
    }

    public SessionState getSession(String id) {
        try {
            return sessionsApi.getSession(id);
        } catch (ApiException e) {
            throw new BrowsingClientException("getSession failed: " + id, e);
        }
    }

    public void deleteSession(String id) {
        try {
            sessionsApi.deleteSession(id);
        } catch (ApiException e) {
            throw new BrowsingClientException("deleteSession failed: " + id, e);
        }
    }

    // --- Navigation + state ------------------------------------------------

    public void navigate(String id, String url) {
        try {
            navigationApi.navigate(id, new NavigateRequest().url(java.net.URI.create(url)));
        } catch (ApiException e) {
            throw new BrowsingClientException("navigate failed: " + id + " -> " + url, e);
        }
    }

    public PageStatus getStatus(String id) {
        try {
            return navigationApi.getPageStatus(id);
        } catch (ApiException e) {
            throw new BrowsingClientException("getPageStatus failed: " + id, e);
        }
    }

    public ViewportState getViewport(String id) {
        try {
            return scrollingApi.getViewport(id);
        } catch (ApiException e) {
            throw new BrowsingClientException("getViewport failed: " + id, e);
        }
    }

    public String getSource(String id) {
        try {
            return navigationApi.getSource(id);
        } catch (ApiException e) {
            throw new BrowsingClientException("getSource failed: " + id, e);
        }
    }

    // --- Screenshots -------------------------------------------------------

    public byte[] screenshot(String id, ScreenshotStrategy strategy) {
        ScreenshotRequest req = new ScreenshotRequest()
            .strategy(strategy)
            .encoding(ScreenshotEncoding.BINARY);
        try {
            File f = screenshotsApi.captureScreenshot(id, req);
            return Files.readAllBytes(f.toPath());
        } catch (ApiException e) {
            throw new BrowsingClientException("captureScreenshot failed: " + id, e);
        } catch (IOException e) {
            throw new BrowsingClientException("read screenshot bytes failed: " + id, e);
        }
    }

    // --- Capture (one-shot) ------------------------------------------------

    public CaptureResponse capture(CaptureRequest req) {
        try {
            return captureApi.capture(req);
        } catch (ApiException e) {
            throw new BrowsingClientException("capture failed", e);
        }
    }

    public byte[] getCaptureScreenshotBytes(String captureId) {
        try {
            File f = captureApi.getCaptureScreenshot(captureId);
            return Files.readAllBytes(f.toPath());
        } catch (ApiException e) {
            throw new BrowsingClientException("getCaptureScreenshot failed: " + captureId, e);
        } catch (IOException e) {
            throw new BrowsingClientException("read capture bytes failed: " + captureId, e);
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
