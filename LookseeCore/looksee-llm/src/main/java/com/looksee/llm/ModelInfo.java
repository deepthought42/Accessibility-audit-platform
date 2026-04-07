package com.looksee.llm;

import lombok.Builder;
import lombok.Value;

/**
 * Static metadata about a resolved model. Cost fields are in USD per 1K tokens
 * and are used by {@link com.looksee.llm.safety.BudgetedLlmClient} to track
 * per-account spend.
 */
@Value
@Builder
public class ModelInfo {
    String provider;
    String modelId;
    int contextWindow;
    boolean supportsVision;
    double inputCostPer1k;
    double outputCostPer1k;
}
