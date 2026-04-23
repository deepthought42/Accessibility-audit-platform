package com.looksee.browsing.client;

import java.time.Duration;

/**
 * Configuration for {@link BrowsingClient}: base URL of browser-service and
 * HTTP timeouts. Constructed from {@code looksee.browsing.*} properties
 * when remote mode is enabled.
 */
public final class BrowsingClientConfig {
    private final String serviceUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public BrowsingClientConfig(String serviceUrl, Duration connectTimeout, Duration readTimeout) {
        if (serviceUrl == null || serviceUrl.isBlank()) {
            throw new IllegalArgumentException(
                "looksee.browsing.service-url must be set when looksee.browsing.mode=remote");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("readTimeout must be positive");
        }
        this.serviceUrl = serviceUrl.endsWith("/")
            ? serviceUrl.substring(0, serviceUrl.length() - 1)
            : serviceUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public String getServiceUrl() { return serviceUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
}
