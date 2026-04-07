package com.looksee.llm.config;

import com.looksee.llm.LlmClient;
import com.looksee.llm.anthropic.AnthropicLlmClient;
import com.looksee.llm.cache.CachingLlmClient;
import com.looksee.llm.metrics.MeteredLlmClient;
import com.looksee.llm.resilience.RetryingLlmClient;
import com.looksee.llm.safety.BudgetedLlmClient;
import com.looksee.llm.safety.LlmBudgetService;
import com.looksee.llm.safety.PiiRedactor;
import com.looksee.llm.safety.RedactingLlmClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Assembles the canonical {@link LlmClient} bean by composing the provider
 * implementation (e.g. {@link AnthropicLlmClient}) with decorators, in this
 * execution order (outermost → innermost):
 *
 * <pre>
 *   Metered → Retrying → Caching → Budgeted → Redacting → Provider
 * </pre>
 *
 * Caching sits inside budget so that cache hits never consume spend; metering
 * is outermost so it observes every decision the stack makes, including
 * content-filter failures.
 *
 * <p>Applications that want a different assembly can define their own
 * {@code @Primary LlmClient} bean; this auto-config backs off when one is
 * already present.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "anthropicLlmClient")
    @ConditionalOnProperty(prefix = "looksee.llm", name = "provider", havingValue = "anthropic", matchIfMissing = true)
    public LlmClient anthropicLlmClient(LlmProperties properties) {
        return new AnthropicLlmClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PiiRedactor piiRedactor() {
        return new PiiRedactor();
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmBudgetService.TierResolver llmTierResolver() {
        // Default: treat everyone as FREE. Applications with an Account graph
        // override this bean to read com.looksee.models.Account.subscriptionTier.
        return accountId -> "FREE";
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmBudgetService llmBudgetService(LlmProperties properties,
                                             LlmBudgetService.TierResolver resolver) {
        return new LlmBudgetService(properties.getBudget(), resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry llmMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "llmClient")
    public LlmClient llmClient(@Qualifier("anthropicLlmClient") LlmClient provider,
                               PiiRedactor redactor,
                               LlmBudgetService budget,
                               LlmProperties properties,
                               MeterRegistry meterRegistry) {
        LlmClient chain = new RedactingLlmClient(provider, redactor,
                properties.getRedaction().isEnabled());
        chain = new BudgetedLlmClient(chain, budget);
        chain = new CachingLlmClient(chain,
                properties.getCache().isEnabled(),
                properties.getCache().getMaxSize(),
                properties.getCache().getTtl());
        chain = new RetryingLlmClient(chain,
                properties.getRetry().getMaxAttempts(),
                properties.getRetry().getInitialBackoffMillis());
        chain = new MeteredLlmClient(chain, meterRegistry);
        return chain;
    }
}
