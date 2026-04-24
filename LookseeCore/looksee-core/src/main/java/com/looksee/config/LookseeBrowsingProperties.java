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

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
