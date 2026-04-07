package com.looksee.llm;

/**
 * Base type for all LLM-layer errors. Unchecked so that it composes cleanly
 * with Resilience4j's retry policy, which distinguishes retryable subclasses
 * from terminal ones.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Provider rate limit (HTTP 429). Retryable.
     */
    public static class LlmRateLimitException extends LlmException {
        public LlmRateLimitException(String message) { super(message); }
        public LlmRateLimitException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Provider refused the request due to safety/content policy. NOT retryable.
     */
    public static class LlmContentFilterException extends LlmException {
        public LlmContentFilterException(String message) { super(message); }
    }

    /**
     * Account has exhausted its daily LLM budget. NOT retryable.
     */
    public static class LlmBudgetExceededException extends LlmException {
        public LlmBudgetExceededException(String message) { super(message); }
    }

    /**
     * Request violates a capability of the resolved model (e.g. asking for
     * vision on a text-only model). NOT retryable.
     */
    public static class LlmCapabilityException extends LlmException {
        public LlmCapabilityException(String message) { super(message); }
    }
}
