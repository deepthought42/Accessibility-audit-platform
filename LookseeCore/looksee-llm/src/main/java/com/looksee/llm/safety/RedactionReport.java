package com.looksee.llm.safety;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import lombok.Value;

/**
 * Per-request summary of what {@link PiiRedactor} removed. Attached to
 * {@link com.looksee.llm.LlmResponse#getMetadata()} under the key
 * {@code "redaction"} so downstream code can audit safety without re-parsing
 * the request.
 */
@Value
public class RedactionReport {

    public enum Category {
        EMAIL, PHONE, CREDIT_CARD, SSN, IP_ADDRESS, JWT, API_KEY,
        HTML_SENSITIVE_INPUT, CSRF_TOKEN, COOKIE, URL_SECRET
    }

    Map<Category, Integer> counts;

    public static RedactionReport empty() {
        return new RedactionReport(Collections.emptyMap());
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnumMap<Category, Integer> counts = new EnumMap<>(Category.class);

        public Builder increment(Category category, int by) {
            if (by > 0) {
                counts.merge(category, by, Integer::sum);
            }
            return this;
        }

        public RedactionReport build() {
            return new RedactionReport(Collections.unmodifiableMap(new EnumMap<>(counts)));
        }
    }
}
