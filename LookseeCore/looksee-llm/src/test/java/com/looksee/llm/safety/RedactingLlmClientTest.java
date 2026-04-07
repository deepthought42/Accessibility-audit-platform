package com.looksee.llm.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.testing.FakeLlmClient;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RedactingLlmClientTest {

    @Test
    void scrubsPromptBeforeForwarding() {
        AtomicReference<LlmRequest> seen = new AtomicReference<>();
        FakeLlmClient fake = new FakeLlmClient().setResponder(req -> {
            seen.set(req);
            return LlmResponse.builder().text("ok").build();
        });
        RedactingLlmClient redacting = new RedactingLlmClient(fake, new PiiRedactor(), true);

        LlmRequest req = LlmRequest.builder()
                .systemPrompt("system")
                .userPrompt("email alice@example.com")
                .build();
        LlmResponse resp = redacting.complete(req);

        assertFalse(seen.get().getUserPrompt().contains("alice@example.com"));
        assertTrue(seen.get().getUserPrompt().contains("[REDACTED:EMAIL]"));
        assertEquals(1, resp.getMetadata().get("redaction.user.total"));
    }

    @Test
    void disabledPassesThrough() {
        AtomicReference<LlmRequest> seen = new AtomicReference<>();
        FakeLlmClient fake = new FakeLlmClient().setResponder(req -> {
            seen.set(req);
            return LlmResponse.builder().text("ok").build();
        });
        RedactingLlmClient redacting = new RedactingLlmClient(fake, new PiiRedactor(), false);

        redacting.complete(LlmRequest.builder().userPrompt("email alice@example.com").build());
        assertTrue(seen.get().getUserPrompt().contains("alice@example.com"));
    }
}
