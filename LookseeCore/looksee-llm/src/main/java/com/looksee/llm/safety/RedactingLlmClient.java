package com.looksee.llm.safety;

import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scrubs {@link LlmRequest#getSystemPrompt()} and {@link LlmRequest#getUserPrompt()}
 * via {@link PiiRedactor} before forwarding. The resulting redaction report is
 * attached to {@link LlmResponse#getMetadata()} under the key {@code "redaction"}.
 *
 * <p>Image payloads are not scrubbed; callers responsible for sensitive imagery
 * should redact pixels upstream.
 */
public class RedactingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final PiiRedactor redactor;
    private final boolean enabled;

    public RedactingLlmClient(LlmClient delegate, PiiRedactor redactor, boolean enabled) {
        this.delegate = delegate;
        this.redactor = redactor;
        this.enabled = enabled;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (!enabled) return delegate.complete(request);
        PiiRedactor.Result sys = redactor.redact(request.getSystemPrompt());
        PiiRedactor.Result usr = redactor.redact(request.getUserPrompt());
        LlmRequest scrubbed = request.toBuilder()
                .systemPrompt(sys.text())
                .userPrompt(usr.text())
                .build();
        LlmResponse resp = delegate.complete(scrubbed);
        return attachReport(resp, sys.report(), usr.report());
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        if (!enabled) return delegate.completeWithVision(request, images);
        PiiRedactor.Result sys = redactor.redact(request.getSystemPrompt());
        PiiRedactor.Result usr = redactor.redact(request.getUserPrompt());
        LlmRequest scrubbed = request.toBuilder()
                .systemPrompt(sys.text())
                .userPrompt(usr.text())
                .build();
        LlmResponse resp = delegate.completeWithVision(scrubbed, images);
        return attachReport(resp, sys.report(), usr.report());
    }

    private LlmResponse attachReport(LlmResponse resp, RedactionReport sys, RedactionReport usr) {
        Map<String, Object> merged = new HashMap<>(resp.getMetadata());
        merged.put("redaction.system.total", sys.total());
        merged.put("redaction.user.total", usr.total());
        merged.put("redaction.user.counts", usr.getCounts());
        return resp.toBuilder().metadata(merged).build();
    }

    @Override public String providerId() { return delegate.providerId(); }
    @Override public ModelInfo modelInfo() { return delegate.modelInfo(); }
}
