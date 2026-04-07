package com.looksee.llm.metrics;

import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmException;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer instrumentation wrapper. Emits:
 * <ul>
 *   <li>{@code llm.requests} (timer) with tags
 *       {@code provider}, {@code model}, {@code feature}, {@code outcome}</li>
 *   <li>{@code llm.tokens.input} / {@code llm.tokens.output} counters</li>
 *   <li>{@code llm.cost.usd} counter</li>
 *   <li>{@code llm.cache.hits} counter</li>
 * </ul>
 *
 * <p>The {@code feature} tag is sourced from {@link LlmRequest#feature()} so
 * downstream teams can slice cost/latency by business feature without changing
 * the metrics surface.
 */
public class MeteredLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final MeterRegistry registry;

    public MeteredLlmClient(LlmClient delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return timed(request, () -> delegate.complete(request));
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        return timed(request, () -> delegate.completeWithVision(request, images));
    }

    private LlmResponse timed(LlmRequest request, java.util.function.Supplier<LlmResponse> call) {
        long start = System.nanoTime();
        String outcome = "success";
        LlmResponse resp = null;
        try {
            resp = call.get();
            return resp;
        } catch (LlmException e) {
            outcome = e.getClass().getSimpleName();
            throw e;
        } finally {
            Tags tags = Tags.of(
                    "provider", delegate.providerId(),
                    "model", delegate.modelInfo().getModelId(),
                    "feature", request.feature(),
                    "outcome", outcome);
            Timer.builder("llm.requests")
                    .tags(tags)
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            if (resp != null) {
                registry.counter("llm.tokens.input", tags).increment(resp.getInputTokens());
                registry.counter("llm.tokens.output", tags).increment(resp.getOutputTokens());
                ModelInfo info = delegate.modelInfo();
                double cost = ((resp.getInputTokens() / 1000.0) * info.getInputCostPer1k())
                        + ((resp.getOutputTokens() / 1000.0) * info.getOutputCostPer1k());
                registry.counter("llm.cost.usd", tags).increment(cost);
                if (resp.isCacheHit()) {
                    registry.counter("llm.cache.hits", tags).increment();
                }
            }
        }
    }

    @Override public String providerId() { return delegate.providerId(); }
    @Override public ModelInfo modelInfo() { return delegate.modelInfo(); }
}
