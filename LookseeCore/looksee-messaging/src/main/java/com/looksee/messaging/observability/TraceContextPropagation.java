package com.looksee.messaging.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

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

    /** Standard W3C {@code traceparent} attribute name. */
    public static final String TRACEPARENT = "traceparent";

    private static final Pattern TRACEPARENT_PATTERN =
        Pattern.compile("^[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

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

    /**
     * Returns the W3C {@code traceparent} string for the currently active
     * OpenTelemetry context, or {@code null} when no span is active.
     *
     * <p>Used by handlers extending {@code PubSubAuditController}: once
     * inside the {@code pubsub.handle.*} span, calling this captures the
     * stable trace-id to stamp onto outbox events so downstream consumers
     * can join the same trace.</p>
     */
    public static String currentTraceparent() {
        Map<String, String> attrs = inject(new HashMap<>());
        return attrs.get(TRACEPARENT);
    }

    /**
     * Returns the {@code traceparent} from the given inbound Pub/Sub
     * attribute map if present and validly formatted, otherwise mints a
     * fresh W3C-compliant {@code traceparent} string.
     *
     * <p>Handlers that do <b>not</b> extend {@code PubSubAuditController}
     * (and therefore have no active span) call this to derive a stable
     * correlation id for outbox events. A freshly minted value still
     * yields a valid W3C header so downstream consumers can extract a
     * parent context without parse errors.</p>
     */
    public static String currentOrMintTraceparent(Map<String, String> inboundAttributes) {
        if (inboundAttributes != null) {
            String inbound = inboundAttributes.get(TRACEPARENT);
            if (inbound != null && TRACEPARENT_PATTERN.matcher(inbound).matches()) {
                return inbound;
            }
        }
        return mintTraceparent();
    }

    /** True when the input parses as a syntactically valid W3C {@code traceparent}. */
    public static boolean isValidTraceparent(String traceparent) {
        return traceparent != null && TRACEPARENT_PATTERN.matcher(traceparent).matches();
    }

    private static String mintTraceparent() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long traceHi = r.nextLong();
        long traceLo = r.nextLong();
        long spanId = r.nextLong();
        // Ensure non-zero trace/span ids (zero is invalid in the W3C spec).
        if (traceHi == 0L && traceLo == 0L) traceLo = 1L;
        if (spanId == 0L) spanId = 1L;
        return String.format("00-%016x%016x-%016x-01", traceHi, traceLo, spanId);
    }
}
