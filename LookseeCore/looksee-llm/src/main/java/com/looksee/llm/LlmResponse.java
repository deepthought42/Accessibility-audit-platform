package com.looksee.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Result of a single {@link LlmClient} invocation.
 *
 * <p>{@code metadata} is used by decorators to attach non-payload information
 * such as redaction reports or budget usage, without changing the shape of the
 * wire response.
 */
@Value
@Builder(toBuilder = true)
public class LlmResponse {

    String text;

    @Builder.Default
    Optional<JsonNode> parsedJson = Optional.empty();

    int inputTokens;
    int outputTokens;

    String finishReason;
    String providerRequestId;
    long latencyMs;

    @With
    boolean cacheHit;

    String modelId;

    @Builder.Default
    Map<String, Object> metadata = Collections.emptyMap();
}
