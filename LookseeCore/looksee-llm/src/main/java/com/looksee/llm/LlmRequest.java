package com.looksee.llm;

import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable request passed to an {@link LlmClient}. Callers should use the
 * Lombok-generated builder.
 *
 * <p>{@code metadata} carries feature-level context (e.g. {@code feature="alt_text"},
 * {@code accountId="..."}) that decorators read for metrics, budget attribution
 * and cache keying. It is never sent to the provider.
 */
@Value
@Builder(toBuilder = true)
public class LlmRequest {

    public enum ResponseFormat { TEXT, JSON_SCHEMA }

    /**
     * Optional system prompt (role/instructions). May be null.
     */
    String systemPrompt;

    /**
     * Required user prompt.
     */
    String userPrompt;

    /**
     * Hard ceiling on output tokens.
     */
    @Builder.Default
    int maxTokens = 1024;

    /**
     * Sampling temperature. Requests at {@code 0.0} are treated as
     * deterministic and are eligible for long-TTL caching.
     */
    @Builder.Default
    double temperature = 0.0;

    @Builder.Default
    ResponseFormat responseFormat = ResponseFormat.TEXT;

    /**
     * JSON schema for {@link ResponseFormat#JSON_SCHEMA} requests. Ignored
     * otherwise. Providers that don't support native schema mode will fall back
     * to prompt-based JSON instruction.
     */
    String jsonSchema;

    /**
     * Optional hint: {@code "default"} (capable model) or {@code "cheap"}
     * (high-volume model). Resolved by the provider implementation.
     */
    @Builder.Default
    String modelHint = "default";

    /**
     * Non-sensitive metadata used by decorators. Must not contain PII.
     * Common keys: {@code feature}, {@code accountId}, {@code traceId}.
     */
    @Builder.Default
    Map<String, String> metadata = Collections.emptyMap();

    public String feature() {
        return metadata.getOrDefault("feature", "unknown");
    }

    public String accountId() {
        return metadata.get("accountId");
    }
}
