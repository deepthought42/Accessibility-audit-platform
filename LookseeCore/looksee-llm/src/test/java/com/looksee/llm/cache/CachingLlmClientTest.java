package com.looksee.llm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.testing.FakeLlmClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CachingLlmClientTest {

    @Test
    void deterministicRequestIsCached() {
        FakeLlmClient fake = new FakeLlmClient();
        CachingLlmClient caching = new CachingLlmClient(fake, true, 100, Duration.ofMinutes(5));
        LlmRequest req = LlmRequest.builder()
                .userPrompt("hello")
                .temperature(0.0)
                .build();

        LlmResponse first = caching.complete(req);
        LlmResponse second = caching.complete(req);

        assertFalse(first.isCacheHit());
        assertTrue(second.isCacheHit());
        assertEquals(1, fake.invocationCount());
    }

    @Test
    void sampledRequestBypassesCache() {
        FakeLlmClient fake = new FakeLlmClient();
        CachingLlmClient caching = new CachingLlmClient(fake, true, 100, Duration.ofMinutes(5));
        LlmRequest req = LlmRequest.builder()
                .userPrompt("hello")
                .temperature(0.7)
                .build();

        caching.complete(req);
        caching.complete(req);

        assertEquals(2, fake.invocationCount());
    }

    @Test
    void disabledCacheBypasses() {
        FakeLlmClient fake = new FakeLlmClient();
        CachingLlmClient caching = new CachingLlmClient(fake, false, 100, Duration.ofMinutes(5));
        LlmRequest req = LlmRequest.builder().userPrompt("hello").build();

        caching.complete(req);
        caching.complete(req);

        assertEquals(2, fake.invocationCount());
    }

    @Test
    void differentPromptsGetDifferentKeys() {
        FakeLlmClient fake = new FakeLlmClient();
        CachingLlmClient caching = new CachingLlmClient(fake, true, 100, Duration.ofMinutes(5));

        caching.complete(LlmRequest.builder().userPrompt("a").build());
        caching.complete(LlmRequest.builder().userPrompt("b").build());

        assertEquals(2, fake.invocationCount());
    }
}
