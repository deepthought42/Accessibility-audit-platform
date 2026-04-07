package com.looksee.llm;

import java.util.List;

/**
 * Provider-agnostic entry point for all LLM calls made by Look-see services.
 *
 * <p>Implementations are expected to be Spring beans. A single {@code @Primary}
 * {@link LlmClient} is assembled by {@link com.looksee.llm.config.LlmAutoConfiguration}
 * by composing the provider implementation with safety, caching, retry and
 * metrics decorators. Callers should always inject the interface, never a
 * concrete implementation.
 */
public interface LlmClient {

    /**
     * Executes a text-only completion against the configured provider.
     */
    LlmResponse complete(LlmRequest request);

    /**
     * Executes a multimodal (text + images) completion. Implementations must
     * fail fast with {@link LlmException} if the resolved model does not
     * report {@link ModelInfo#isSupportsVision()}.
     */
    LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images);

    /**
     * Stable identifier for the underlying provider, e.g. {@code "anthropic"}.
     * Used as a metrics tag and as part of the cache key.
     */
    String providerId();

    /**
     * Metadata about the model that will be used if {@link LlmRequest#getModelHint()}
     * is not set. Decorators inspect this for cost bookkeeping and capability checks.
     */
    ModelInfo modelInfo();
}
