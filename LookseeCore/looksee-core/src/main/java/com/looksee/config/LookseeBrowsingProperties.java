package com.looksee.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config binding for {@code looksee.browsing.*}.
 *
 * <p>Default mode is {@link Mode#LOCAL} — every existing LookseeCore consumer
 * picks up 0.6.0 without any behavior change. Set
 * {@code looksee.browsing.mode=remote} + {@code looksee.browsing.service-url}
 * to route browsing operations through brandonkindred/browser-service.
 */
@ConfigurationProperties("looksee.browsing")
public class LookseeBrowsingProperties {

    public enum Mode { LOCAL, REMOTE }

    private Mode mode = Mode.LOCAL;
    private String serviceUrl;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(120);
    private final SmokeCheck smokeCheck = new SmokeCheck();

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public SmokeCheck getSmokeCheck() { return smokeCheck; }

    /**
     * Phase 4a.4: opt-in periodic {@code browserService.capturePage} probe
     * used as a watchdog during phase-4 staging burn-in and prod cutover.
     * Disabled by default; the {@code CapturePageSmokeCheck} bean isn't
     * created when {@link #isEnabled()} is false.
     */
    public static class SmokeCheck {
        private boolean enabled = false;
        private Duration interval = Duration.ofSeconds(60);
        private String targetUrl = "https://example.com";
        private com.looksee.browser.enums.BrowserType browser = com.looksee.browser.enums.BrowserType.CHROME;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Duration getInterval() { return interval; }
        public void setInterval(Duration interval) { this.interval = interval; }

        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

        public com.looksee.browser.enums.BrowserType getBrowser() { return browser; }
        public void setBrowser(com.looksee.browser.enums.BrowserType browser) { this.browser = browser; }
    }
}
