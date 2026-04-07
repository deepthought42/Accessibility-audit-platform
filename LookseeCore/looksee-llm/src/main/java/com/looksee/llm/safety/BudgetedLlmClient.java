package com.looksee.llm.safety;

import com.looksee.llm.ImagePart;
import com.looksee.llm.LlmClient;
import com.looksee.llm.LlmException.LlmBudgetExceededException;
import com.looksee.llm.LlmRequest;
import com.looksee.llm.LlmResponse;
import com.looksee.llm.ModelInfo;
import java.util.List;

/**
 * Enforces per-account daily USD caps. Throws
 * {@link LlmBudgetExceededException} <b>before</b> hitting the provider when
 * the account is already over budget, and records actual cost after every
 * successful call using the resolved model's per-1K rates.
 */
public class BudgetedLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final LlmBudgetService budget;

    public BudgetedLlmClient(LlmClient delegate, LlmBudgetService budget) {
        this.delegate = delegate;
        this.budget = budget;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        checkBudget(request);
        LlmResponse resp = delegate.complete(request);
        chargeIfLive(request, resp);
        return resp;
    }

    @Override
    public LlmResponse completeWithVision(LlmRequest request, List<ImagePart> images) {
        checkBudget(request);
        LlmResponse resp = delegate.completeWithVision(request, images);
        chargeIfLive(request, resp);
        return resp;
    }

    private void checkBudget(LlmRequest request) {
        if (!budget.hasHeadroom(request.accountId())) {
            throw new LlmBudgetExceededException(
                    "Daily LLM budget exhausted for account=" + request.accountId()
                            + " feature=" + request.feature());
        }
    }

    private void chargeIfLive(LlmRequest request, LlmResponse resp) {
        if (resp.isCacheHit()) {
            return; // cache hits never consume budget
        }
        ModelInfo info = delegate.modelInfo();
        double cost = ((resp.getInputTokens() / 1000.0) * info.getInputCostPer1k())
                + ((resp.getOutputTokens() / 1000.0) * info.getOutputCostPer1k());
        budget.recordSpend(request.accountId(), cost);
    }

    @Override public String providerId() { return delegate.providerId(); }
    @Override public ModelInfo modelInfo() { return delegate.modelInfo(); }
}
