package com.looksee.llm.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.looksee.llm.LlmException;
import com.looksee.llm.LlmException.LlmContentFilterException;
import com.looksee.llm.LlmException.LlmRateLimitException;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.testing.FakeLlmClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetryingLlmClientTest {

    private final LlmRequest request = LlmRequest.builder().userPrompt("hi").build();

    @Test
    void retriesOnRateLimitThenSucceeds() {
        FakeLlmClient fake = new FakeLlmClient()
                .throwNext(2, new LlmRateLimitException("slow down"));
        List<Long> sleeps = new ArrayList<>();
        RetryingLlmClient retrying = new RetryingLlmClient(fake, 4, 10L, sleeps::add);

        LlmResponse resp = retrying.complete(request);

        assertEquals("ok", resp.getText());
        assertEquals(3, fake.invocationCount());
        // Backoff doubles: 10, 20 between the three attempts.
        assertEquals(List.of(10L, 20L), sleeps);
    }

    @Test
    void contentFilterIsNotRetried() {
        FakeLlmClient fake = new FakeLlmClient()
                .throwNext(1, new LlmContentFilterException("nope"));
        List<Long> sleeps = new ArrayList<>();
        RetryingLlmClient retrying = new RetryingLlmClient(fake, 4, 10L, sleeps::add);

        assertThrows(LlmContentFilterException.class, () -> retrying.complete(request));
        assertEquals(1, fake.invocationCount());
        assertEquals(0, sleeps.size());
    }

    @Test
    void exhaustsAttemptsAndWraps() {
        FakeLlmClient fake = new FakeLlmClient()
                .throwNext(10, new LlmRateLimitException("429"));
        RetryingLlmClient retrying = new RetryingLlmClient(fake, 3, 1L, ms -> {});

        assertThrows(LlmException.class, () -> retrying.complete(request));
        assertEquals(3, fake.invocationCount());
    }

    @Test
    void retriesOnIoExceptionWrappedInLlmException() {
        FakeLlmClient fake = new FakeLlmClient()
                .throwNext(1, new LlmException("network", new IOException("boom")));
        RetryingLlmClient retrying = new RetryingLlmClient(fake, 3, 1L, ms -> {});

        LlmResponse resp = retrying.complete(request);
        assertEquals("ok", resp.getText());
        assertEquals(2, fake.invocationCount());
    }
}
