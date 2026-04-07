package com.looksee.llm.safety;

import com.looksee.llm.config.LlmProperties;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * In-memory daily USD budget tracker keyed on {@code accountId}. This is a
 * deliberate first-pass: it's correct within a single service instance and
 * fast. A future revision can back it with Redis or Neo4j for cross-instance
 * accounting.
 *
 * <p>Budgets come from {@link LlmProperties.Budget}: {@code defaultDailyUsd}
 * with per-tier overrides. Tier lookup is pluggable via
 * {@link TierResolver} so tests don't need a full Neo4j graph.
 */
public class LlmBudgetService {

    /**
     * Resolves an account id to a subscription tier name (e.g. {@code "FREE"},
     * {@code "PRO"}, {@code "ENTERPRISE"}). Implementations typically read
     * {@code com.looksee.models.Account.subscriptionTier}.
     */
    public interface TierResolver {
        String resolve(String accountId);
    }

    private final LlmProperties.Budget config;
    private final TierResolver tierResolver;
    private final Map<String, DoubleAdder> dailySpend = new ConcurrentHashMap<>();
    private volatile LocalDate bucketDate = LocalDate.now(ZoneOffset.UTC);

    public LlmBudgetService(LlmProperties.Budget config, TierResolver tierResolver) {
        this.config = config;
        this.tierResolver = tierResolver;
    }

    /**
     * True if {@code accountId} has at least one cent of headroom remaining
     * today. A null or unknown account gets the default budget.
     */
    public boolean hasHeadroom(String accountId) {
        rotateIfNewDay();
        double spent = dailySpend.getOrDefault(keyFor(accountId), new DoubleAdder()).sum();
        return spent < limitFor(accountId);
    }

    /**
     * Records that {@code costUsd} was spent for {@code accountId}. Must be
     * called by the caller after every successful provider invocation.
     */
    public void recordSpend(String accountId, double costUsd) {
        rotateIfNewDay();
        dailySpend.computeIfAbsent(keyFor(accountId), k -> new DoubleAdder()).add(costUsd);
    }

    public double remainingUsd(String accountId) {
        rotateIfNewDay();
        double spent = dailySpend.getOrDefault(keyFor(accountId), new DoubleAdder()).sum();
        return Math.max(0.0, limitFor(accountId) - spent);
    }

    private double limitFor(String accountId) {
        String tier = accountId == null ? null : tierResolver.resolve(accountId);
        if (tier != null && config.getTierOverrides().containsKey(tier)) {
            return config.getTierOverrides().get(tier);
        }
        return config.getDefaultDailyUsd();
    }

    private String keyFor(String accountId) {
        return accountId == null ? "__anonymous__" : accountId;
    }

    private synchronized void rotateIfNewDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(bucketDate)) {
            dailySpend.clear();
            bucketDate = today;
        }
    }
}
