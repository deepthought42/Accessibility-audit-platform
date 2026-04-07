package com.looksee.llm.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmException;
import com.looksee.llm.LlmException.LlmCapabilityException;
import com.looksee.llm.LlmException.LlmContentFilterException;
import com.looksee.llm.LlmException.LlmRateLimitException;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import com.looksee.llm.config.LlmProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Anthropic implementation of {@link LlmClient}.
 *
 * <p>Uses the JDK's built-in {@link HttpClient} rather than pulling in a
 * third-party SDK — this keeps the dependency surface minimal and avoids
 * coupling the shared library to a specific SDK release cadence. The wire
 * format follows the public Anthropic Messages API.
 *
 * <p>Exceptions are mapped as follows:
 * <ul>
 *   <li>HTTP 429 &rarr; {@link LlmRateLimitException} (retryable)</li>
 *   <li>HTTP 400 with {@code type=="invalid_request_error"} mentioning content policy
 *       &rarr; {@link LlmContentFilterException} (terminal)</li>
 *   <li>Any other error &rarr; generic {@link LlmException}</li>
 * </ul>
 */
@Slf4j
public class AnthropicLlmClient implements LlmClient {

    private static final String MESSAGES_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final LlmProperties properties;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnthropicLlmClient(LlmProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Visible for testing.
    AnthropicLlmClient(LlmProperties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public String providerId() {
        return "anthropic";
    }

    @Override
    public ModelInfo modelInfo() {
        return modelInfoFor(properties.getAnthropic().getDefaultModel());
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return invoke(request, Collections.emptyList());
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        ModelInfo info = modelInfoFor(resolveModelId(request));
        if (!info.isSupportsVision()) {
            throw new LlmCapabilityException(
                    "Model " + info.getModelId() + " does not support vision input");
        }
        return invoke(request, images);
    }

    private LlmResponse invoke(LlmRequest request, List<ImagePart> images) {
        String modelId = resolveModelId(request);
        ObjectNode body = buildRequestBody(request, images, modelId);

        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MESSAGES_ENDPOINT))
                    .timeout(Duration.ofSeconds(60))
                    .header("x-api-key", properties.getAnthropic().getApiKey())
                    .header("anthropic-version", API_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
        } catch (IOException e) {
            throw new LlmException("Failed to serialize Anthropic request", e);
        }

        long start = System.currentTimeMillis();
        HttpResponse<String> resp;
        try {
            resp = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // IOException is retryable by RetryingLlmClient.
            throw new LlmException("Anthropic request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Anthropic request interrupted", e);
        }
        long latency = System.currentTimeMillis() - start;

        mapErrorIfAny(resp);
        return parseResponse(resp.body(), modelId, latency, request);
    }

    private ObjectNode buildRequestBody(LlmRequest request, List<ImagePart> images, String modelId) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelId);
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");

        ArrayNode content = userMsg.putArray("content");
        for (ImagePart image : images) {
            ObjectNode imgNode = content.addObject();
            imgNode.put("type", "image");
            ObjectNode source = imgNode.putObject("source");
            source.put("type", "base64");
            source.put("media_type", image.getMimeType());
            source.put("data", Base64.getEncoder().encodeToString(image.getBytes()));
        }
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        textNode.put("text", userPromptWithFormatInstruction(request));
        return body;
    }

    private String userPromptWithFormatInstruction(LlmRequest request) {
        if (request.getResponseFormat() == LlmRequest.ResponseFormat.JSON_SCHEMA
                && request.getJsonSchema() != null) {
            return request.getUserPrompt()
                    + "\n\nRespond ONLY with valid JSON conforming to this schema:\n"
                    + request.getJsonSchema();
        }
        return request.getUserPrompt();
    }

    private void mapErrorIfAny(HttpResponse<String> resp) {
        int status = resp.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String body = resp.body() == null ? "" : resp.body();
        if (status == 429) {
            throw new LlmRateLimitException("Anthropic rate limited: " + body);
        }
        if (body.contains("content policy") || body.contains("content_policy")) {
            throw new LlmContentFilterException("Anthropic content filter: " + body);
        }
        throw new LlmException("Anthropic HTTP " + status + ": " + body);
    }

    private LlmResponse parseResponse(String body, String modelId, long latencyMs, LlmRequest request) {
        try {
            JsonNode root = mapper.readTree(body);
            String text = "";
            JsonNode contentArr = root.path("content");
            if (contentArr.isArray() && contentArr.size() > 0) {
                JsonNode first = contentArr.get(0);
                text = first.path("text").asText("");
            }
            int inputTokens = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);
            String stopReason = root.path("stop_reason").asText(null);
            String providerRequestId = root.path("id").asText(null);

            Optional<JsonNode> parsedJson = Optional.empty();
            if (request.getResponseFormat() == LlmRequest.ResponseFormat.JSON_SCHEMA) {
                try {
                    parsedJson = Optional.of(mapper.readTree(text));
                } catch (IOException ignored) {
                    log.warn("LLM returned non-JSON body for JSON_SCHEMA request; feature={} ",
                            request.feature());
                }
            }

            return LlmResponse.builder()
                    .text(text)
                    .parsedJson(parsedJson)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .finishReason(stopReason)
                    .providerRequestId(providerRequestId)
                    .latencyMs(latencyMs)
                    .cacheHit(false)
                    .modelId(modelId)
                    .build();
        } catch (IOException e) {
            throw new LlmException("Failed to parse Anthropic response", e);
        }
    }

    private String resolveModelId(LlmRequest request) {
        if ("cheap".equalsIgnoreCase(request.getModelHint())) {
            return properties.getAnthropic().getCheapModel();
        }
        return properties.getAnthropic().getDefaultModel();
    }

    private ModelInfo modelInfoFor(String modelId) {
        // Static table; costs approximate and used only for internal budget
        // accounting. Update when pricing changes.
        if (modelId == null) {
            modelId = "claude-opus-4-6";
        }
        boolean vision = modelId.contains("opus") || modelId.contains("sonnet") || modelId.contains("haiku");
        double in = modelId.contains("opus") ? 0.015 : modelId.contains("sonnet") ? 0.003 : 0.001;
        double out = modelId.contains("opus") ? 0.075 : modelId.contains("sonnet") ? 0.015 : 0.005;
        return ModelInfo.builder()
                .provider("anthropic")
                .modelId(modelId)
                .contextWindow(200_000)
                .supportsVision(vision)
                .inputCostPer1k(in)
                .outputCostPer1k(out)
                .build();
    }
}
