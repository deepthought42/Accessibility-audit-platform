package com.looksee.llm.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.llm.LlmException.LlmBudgetExceededException;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.config.LlmProperties;
import com.looksee.llm.testing.FakeLlmClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetedLlmClientTest {

    private LlmBudgetService service(double defaultUsd) {
        LlmProperties.Budget cfg = new LlmProperties.Budget();
        cfg.setDefaultDailyUsd(defaultUsd);
        return new LlmBudgetService(cfg, accountId -> "FREE");
    }

    private LlmRequest request(String accountId) {
        return LlmRequest.builder()
                .userPrompt("hi")
                .metadata(Map.of("accountId", accountId, "feature", "test"))
                .build();
    }

    @Test
    void allowsCallsUnderLimit() {
        FakeLlmClient fake = new FakeLlmClient().enqueue(
                LlmResponse.builder().text("ok").inputTokens(100).outputTokens(50).build());
        BudgetedLlmClient budgeted = new BudgetedLlmClient(fake, service(10.0));

        LlmResponse resp = budgeted.complete(request("acct-1"));
        assertEquals("ok", resp.getText());
    }

    @Test
    void throwsOnceBudgetExhausted() {
        LlmBudgetService svc = service(0.0001);
        // Burn the budget preemptively.
        svc.recordSpend("acct-1", 1.0);

        FakeLlmClient fake = new FakeLlmClient();
        BudgetedLlmClient budgeted = new BudgetedLlmClient(fake, svc);

        assertThrows(LlmBudgetExceededException.class,
                () -> budgeted.complete(request("acct-1")));
        assertEquals(0, fake.invocationCount());
    }

    @Test
    void cacheHitsDoNotChargeBudget() {
        LlmBudgetService svc = service(10.0);
        FakeLlmClient fake = new FakeLlmClient().enqueue(
                LlmResponse.builder()
                        .text("cached")
                        .inputTokens(1000)
                        .outputTokens(1000)
                        .cacheHit(true)
                        .build());
        BudgetedLlmClient budgeted = new BudgetedLlmClient(fake, svc);

        budgeted.complete(request("acct-1"));
        assertEquals(10.0, svc.remainingUsd("acct-1"), 0.0001);
    }

    @Test
    void liveCallsDecrementBudget() {
        LlmBudgetService svc = service(10.0);
        FakeLlmClient fake = new FakeLlmClient().enqueue(
                LlmResponse.builder()
                        .text("x")
                        .inputTokens(1000) // 1000 tokens * 0.001/1k = 0.001 USD
                        .outputTokens(1000) // 1000 tokens * 0.002/1k = 0.002 USD
                        .build());
        BudgetedLlmClient budgeted = new BudgetedLlmClient(fake, svc);

        budgeted.complete(request("acct-1"));
        assertTrue(svc.remainingUsd("acct-1") < 10.0);
        assertTrue(svc.remainingUsd("acct-1") > 9.99);
    }
}
