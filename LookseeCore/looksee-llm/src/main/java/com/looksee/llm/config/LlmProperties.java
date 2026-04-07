package com.looksee.llm.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All configuration for the LLM layer lives here. Bound via Spring Boot's
 * {@code @ConfigurationProperties} from {@code application.yml} under the
 * {@code looksee.llm} prefix. See {@code README} in this module for an example.
 */
@Data
@ConfigurationProperties("looksee.llm")
public class LlmProperties {

    private String provider = "anthropic";
    private Anthropic anthropic = new Anthropic();
    private Cache cache = new Cache();
    private Budget budget = new Budget();
    private Redaction redaction = new Redaction();
    private Retry retry = new Retry();

    @Data
    public static class Anthropic {
        private String apiKey;
        private String defaultModel = "claude-opus-4-6";
        private String cheapModel = "claude-haiku-4-5-20251001";
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private int maxSize = 10_000;
        private Duration ttl = Duration.ofDays(7);
    }

    @Data
    public static class Budget {
        private double defaultDailyUsd = 5.00;
        private Map<String, Double> tierOverrides = new HashMap<>();
    }

    @Data
    public static class Redaction {
        private boolean enabled = true;
        private boolean strict = false;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 4;
        private long initialBackoffMillis = 1000L;
    }
}
