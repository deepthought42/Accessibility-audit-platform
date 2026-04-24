package com.looksee.services.browser;

import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.AlertChoice;
import org.openqa.selenium.Alert;

/**
 * {@link Alert} bound to a remote browser-service session. Text is cached from
 * the {@code GET /v1/sessions/{id}/alert} response; {@link #accept()} and
 * {@link #dismiss()} forward to {@code POST /v1/sessions/{id}/alert/respond}.
 *
 * <p>{@link #sendKeys(String)} is deferred to phase 3c — the current consumer
 * census has no callers, so it's not wired. If a future caller surfaces, add
 * an {@code input} field to the respond request in the same commit as the
 * caller.
 */
public final class RemoteAlert implements Alert {

    private final BrowsingClient client;
    private final String sessionId;
    private final String cachedText;

    public RemoteAlert(BrowsingClient client, String sessionId, String cachedText) {
        this.client = client;
        this.sessionId = sessionId;
        this.cachedText = cachedText;
    }

    @Override
    public String getText() {
        return cachedText;
    }

    @Override
    public void accept() {
        client.respondToAlert(sessionId, AlertChoice.ACCEPT, null);
    }

    @Override
    public void dismiss() {
        client.respondToAlert(sessionId, AlertChoice.DISMISS, null);
    }

    @Override
    public void sendKeys(String keysToSend) {
        throw new UnsupportedOperationException(
            "RemoteAlert.sendKeys is deferred to phase 3c "
            + "(current consumer census has no callers)");
    }
}
