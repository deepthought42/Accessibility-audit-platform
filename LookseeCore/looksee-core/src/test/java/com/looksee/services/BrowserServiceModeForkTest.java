package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.Session;
import com.looksee.config.LookseeBrowsingProperties;
import com.looksee.services.browser.RemoteBrowser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Guards the two invariants of the mode-flag fork on {@link BrowserService}:
 * remote mode returns a {@link RemoteBrowser}; local mode (the default) does
 * not touch {@link BrowsingClient} at all. No Spring context is required —
 * we construct the service directly and inject the two flag-relevant fields
 * via {@link ReflectionTestUtils}.
 */
class BrowserServiceModeForkTest {

    @Test
    void remoteMode_returnsRemoteBrowserAndCreatesSession() throws Exception {
        BrowsingClient client = mock(BrowsingClient.class);
        when(client.createSession(any(), any()))
            .thenReturn(new Session().sessionId("sess-1"));

        LookseeBrowsingProperties props = new LookseeBrowsingProperties();
        props.setMode(LookseeBrowsingProperties.Mode.REMOTE);

        BrowserService svc = new BrowserService();
        ReflectionTestUtils.setField(svc, "browsingProps", props);
        ReflectionTestUtils.setField(svc, "browsingClient", client);

        Browser b = svc.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);

        assertInstanceOf(RemoteBrowser.class, b);
        assertEquals("sess-1", ((RemoteBrowser) b).getSessionId());
        verify(client).createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
    }

    @Test
    void localMode_doesNotTouchBrowsingClient() {
        BrowsingClient client = mock(BrowsingClient.class);
        LookseeBrowsingProperties props = new LookseeBrowsingProperties(); // default LOCAL

        BrowserService svc = new BrowserService();
        ReflectionTestUtils.setField(svc, "browsingProps", props);
        ReflectionTestUtils.setField(svc, "browsingClient", client);

        // getConnection in local mode calls BrowserConnectionHelper which will
        // try to talk to a Selenium hub — that's expected to fail here. The
        // contract we verify is that the BrowsingClient is never consulted.
        try {
            svc.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
        } catch (Throwable ignored) {
            // Local path is byte-identical to 0.5.0; any downstream error is
            // orthogonal to the mode fork.
        }
        verifyNoInteractions(client);
    }

    @Test
    void nullProps_fallsBackToLocal() {
        BrowsingClient client = mock(BrowsingClient.class);
        BrowserService svc = new BrowserService();
        // browsingProps left null — Spring may not have wired it yet / absent
        ReflectionTestUtils.setField(svc, "browsingClient", client);

        try {
            svc.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
        } catch (Throwable ignored) {
            // same as above — we only care that we did NOT go remote
        }
        verifyNoInteractions(client);
    }
}
