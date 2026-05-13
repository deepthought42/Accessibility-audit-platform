package com.looksee.services;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.looksee.messaging.TracingPubSubPublisher;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.models.OutboxEvent;
import com.looksee.models.OutboxEventStatus;
import com.looksee.models.OutboxSerializationException;
import com.looksee.models.repository.OutboxEventRepository;

/**
 * Polls for due {@link OutboxEvent} records and publishes them to Pub/Sub.
 *
 * <p>This implements the Transactional Outbox pattern: services write an
 * OutboxEvent in the same Neo4j transaction as their domain changes through
 * {@link OutboxPublishingGateway}, and this publisher asynchronously delivers
 * them to Pub/Sub with exponential backoff and W3C trace-context propagation.</p>
 *
 * <p><b>Backoff:</b> after each failed attempt the event's
 * {@code nextAttemptAt} is advanced by {@code 5 · 2^retryCount} seconds, so
 * subsequent attempts space out at 10s / 20s / 40s / 80s. After 5 failed
 * attempts the event is flipped to {@code FAILED} and excluded from polling;
 * the daily {@code cleanupOldFailedEvents} sweeper removes rows older than
 * 30 days.</p>
 *
 * <p><b>Tracing:</b> publish uses
 * {@link TracingPubSubPublisher#publishWithCorrelation} so consumers see the
 * trace of the handler that originally wrote the event, not the (unrelated)
 * trace of this poller thread.</p>
 */
@Service
public class OutboxEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    static final int MAX_RETRIES = 5;

    @Autowired(required = false)
    private TracingPubSubPublisher tracingPubSubPublisher;

    @Autowired(required = false)
    private OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private PubSubMetrics pubSubMetrics;

    private final AtomicBoolean gaugeRegistered = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        if (tracingPubSubPublisher == null || outboxEventRepository == null) {
            return;
        }
        registerPendingGaugeIfNeeded();

        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> due = outboxEventRepository.findDueEvents(now);
        for (OutboxEvent event : due) {
            attemptPublish(event);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldEvents() {
        if (outboxEventRepository == null) {
            return;
        }
        outboxEventRepository.deleteOldProcessedEvents();
        log.info("Cleaned up old processed outbox events");
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanupOldFailedEvents() {
        if (outboxEventRepository == null) {
            return;
        }
        outboxEventRepository.deleteOldFailedEvents();
        log.info("Cleaned up old failed outbox events");
    }

    private void attemptPublish(OutboxEvent event) {
        try {
            tracingPubSubPublisher
                .publishWithCorrelation(event.getTopic(), event.getPayload(), event.getCorrelationId())
                .get();
            markProcessed(event);
        } catch (Exception e) {
            markFailedAttempt(event, e);
        }
    }

    private void markProcessed(OutboxEvent event) {
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(OutboxEventStatus.PROCESSED.name());
        event.setProcessedAt(now);
        outboxEventRepository.save(event);
        if (pubSubMetrics != null) {
            pubSubMetrics.recordOutboxPublished(event.getTopic(), "success");
            if (event.getCreatedAt() != null) {
                long lagNanos = ChronoUnit.NANOS.between(event.getCreatedAt(), now);
                if (lagNanos >= 0) {
                    pubSubMetrics.recordOutboxLag(event.getTopic(), lagNanos);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("outbox event published: eventId={} topic={}",
                event.getEventId(), event.getTopic());
        }
    }

    private void markFailedAttempt(OutboxEvent event, Exception cause) {
        int nextRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(nextRetryCount);
        String reason = classifyFailure(cause);

        if (nextRetryCount >= MAX_RETRIES) {
            event.setStatus(OutboxEventStatus.FAILED.name());
            event.setProcessedAt(LocalDateTime.now());
            log.error("outbox event permanently failed: eventId={} topic={} attempts={}",
                event.getEventId(), event.getTopic(), nextRetryCount, cause);
            if (pubSubMetrics != null) {
                pubSubMetrics.recordOutboxPublished(event.getTopic(), "exhausted");
                pubSubMetrics.recordOutboxFailed(event.getTopic(), "exhausted");
            }
        } else {
            Duration backoff = Duration.ofSeconds(5L << nextRetryCount);
            event.setNextAttemptAt(LocalDateTime.now().plus(backoff));
            log.warn("outbox event publish failed, will retry: eventId={} topic={} attempt={} nextAttemptAt={} reason={}",
                event.getEventId(), event.getTopic(), nextRetryCount, event.getNextAttemptAt(), reason, cause);
            if (pubSubMetrics != null) {
                pubSubMetrics.recordOutboxPublished(event.getTopic(), "retrying");
                pubSubMetrics.recordOutboxFailed(event.getTopic(), reason);
            }
        }
        outboxEventRepository.save(event);
    }

    private void registerPendingGaugeIfNeeded() {
        if (pubSubMetrics == null || outboxEventRepository == null) {
            return;
        }
        if (gaugeRegistered.compareAndSet(false, true)) {
            pubSubMetrics.registerOutboxPendingGauge(
                () -> outboxEventRepository.countDueEvents(LocalDateTime.now()));
        }
    }

    /** Best-effort exception-type → metric-reason classification. */
    private static String classifyFailure(Throwable cause) {
        Throwable t = cause;
        while (t != null) {
            String name = t.getClass().getName();
            if (t instanceof OutboxSerializationException
                    || name.contains("JsonProcessing")) {
                return "serialization";
            }
            if (t instanceof IOException
                    || name.startsWith("com.google.api.gax")
                    || name.contains("StatusRuntimeException")
                    || name.contains("Unavailable")) {
                return "pubsub_unavailable";
            }
            t = t.getCause();
        }
        return "unknown";
    }
}
