package com.looksee.messaging.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * W3C trace-context propagation helpers for Pub/Sub messages.
 *
 * <p>Pub/Sub messages carry an optional {@code attributes} map. By injecting
 * the active span's W3C {@code traceparent}/{@code tracestate} headers into
 * those attributes on the publish side, and extracting them on the consume
 * side, downstream services can build a single distributed trace that spans
 * the entire pipeline (PageBuilder → AuditManager → contentAudit etc.).</p>
 *
 * <p>This is the Wave 2 plumbing that the {@link PubSubMetrics} dashboards
 * and the upcoming {@code PubSubAuditController} base class rely on.</p>
 */
public final class TraceContextPropagation {

    private static final TextMapSetter<Map<String, String>> SETTER =
        (carrier, key, value) -> {
            if (carrier != null) {
                carrier.put(key, value);
            }
        };

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier == null ? Collections.emptyList() : carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    private TraceContextPropagation() {}

    /**
     * Inject the current OpenTelemetry context into the given attribute map.
     * Safe to call when no span is active; in that case the map is unchanged.
     *
     * @return the map for chaining (a new HashMap when {@code attributes} is null)
     */
    public static Map<String, String> inject(Map<String, String> attributes) {
        Map<String, String> target = attributes != null ? attributes : new HashMap<>();
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(Context.current(), target, SETTER);
        return target;
    }

    /**
     * Extract a parent OpenTelemetry context from the given attribute map.
     * Returns {@link Context#current()} when no propagated context is present.
     */
    public static Context extract(Map<String, String> attributes) {
        return GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .extract(Context.current(), attributes, GETTER);
    }
}
