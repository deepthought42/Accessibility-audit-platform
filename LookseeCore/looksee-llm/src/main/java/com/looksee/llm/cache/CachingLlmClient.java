package com.looksee.llm.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Caffeine-backed cache in front of an {@link LlmClient}.
 *
 * <p>Only deterministic requests ({@code temperature == 0.0}) are cached;
 * sampled requests bypass the cache so callers never get stale "creative"
 * output. Cache hits return a copy of the stored response with
 * {@link LlmResponse#isCacheHit()} set to {@code true} — decorators upstream
 * (notably {@link com.looksee.llm.safety.BudgetedLlmClient}) rely on this flag
 * to avoid double-charging.
 */
public class CachingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final Cache<String, LlmResponse> cache;
    private final boolean enabled;

    public CachingLlmClient(LlmClient delegate, boolean enabled, int maxSize, Duration ttl) {
        this.delegate = delegate;
        this.enabled = enabled;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (!enabled || request.getTemperature() != 0.0) {
            return delegate.complete(request);
        }
        String key = keyFor(request, List.of());
        LlmResponse cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached.withCacheHit(true);
        }
        LlmResponse resp = delegate.complete(request);
        cache.put(key, resp);
        return resp;
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        if (!enabled || request.getTemperature() != 0.0) {
            return delegate.completeWithVision(request, images);
        }
        String key = keyFor(request, images);
        LlmResponse cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached.withCacheHit(true);
        }
        LlmResponse resp = delegate.completeWithVision(request, images);
        cache.put(key, resp);
        return resp;
    }

    long size() {
        return cache.estimatedSize();
    }

    private String keyFor(LlmRequest request, List<ImagePart> images) {
        StringBuilder sb = new StringBuilder();
        sb.append(delegate.providerId()).append('|');
        sb.append(delegate.modelInfo().getModelId()).append('|');
        sb.append(request.getModelHint()).append('|');
        sb.append(request.getResponseFormat()).append('|');
        sb.append(Objects.toString(request.getJsonSchema(), "")).append('|');
        sb.append(Objects.toString(request.getSystemPrompt(), "")).append('|');
        sb.append(request.getUserPrompt()).append('|');
        for (ImagePart img : images) {
            sb.append(img.sha256()).append('|');
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override public String providerId() { return delegate.providerId(); }
    @Override public ModelInfo modelInfo() { return delegate.modelInfo(); }
}
