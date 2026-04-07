package com.looksee.llm.testing;

import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Deterministic, no-network {@link LlmClient} for unit tests.
 *
 * <p>Usage: either script responses with {@link #enqueue(LlmResponse)} for
 * sequential scenarios, or install a {@link #setResponder(Function) responder}
 * for input-dependent behavior. Exposes a {@link #invocationCount()} so tests
 * can assert that caching wrappers did (or did not) call through.
 */
public class FakeLlmClient implements LlmClient {

    private final List<LlmResponse> queue = new ArrayList<>();
    private final AtomicInteger invocations = new AtomicInteger();
    private Function<LlmRequest, LlmResponse> responder;
    private RuntimeException throwOnNextN;
    private int throwsRemaining = 0;

    private final ModelInfo modelInfo = ModelInfo.builder()
            .provider("fake")
            .modelId("fake-model")
            .contextWindow(100_000)
            .supportsVision(true)
            .inputCostPer1k(0.001)
            .outputCostPer1k(0.002)
            .build();

    public FakeLlmClient enqueue(LlmResponse response) {
        queue.add(response);
        return this;
    }

    public FakeLlmClient setResponder(Function<LlmRequest, LlmResponse> responder) {
        this.responder = responder;
        return this;
    }

    /**
     * The next {@code times} invocations will throw {@code ex} before
     * returning to normal behavior.
     */
    public FakeLlmClient throwNext(int times, RuntimeException ex) {
        this.throwsRemaining = times;
        this.throwOnNextN = ex;
        return this;
    }

    public int invocationCount() {
        return invocations.get();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return respond(request);
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        return respond(request);
    }

    private LlmResponse respond(LlmRequest request) {
        invocations.incrementAndGet();
        if (throwsRemaining > 0) {
            throwsRemaining--;
            throw throwOnNextN;
        }
        if (responder != null) {
            return responder.apply(request);
        }
        if (!queue.isEmpty()) {
            return queue.remove(0);
        }
        return LlmResponse.builder()
                .text("ok")
                .inputTokens(10)
                .outputTokens(5)
                .finishReason("end_turn")
                .providerRequestId("fake-" + invocations.get())
                .latencyMs(1)
                .modelId("fake-model")
                .build();
    }

    @Override public String providerId() { return "fake"; }
    @Override public ModelInfo modelInfo() { return modelInfo; }
}
