package com.looksee.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.looksee.models.OutboxEvent;
import com.looksee.models.repository.OutboxEventRepository;

/**
 * Polls for pending {@link OutboxEvent} records and publishes them to PubSub.
 *
 * <p>This implements the Transactional Outbox pattern: services write an
 * OutboxEvent in the same Neo4j transaction as their domain changes, and
 * this publisher asynchronously delivers them to PubSub with retry logic.
 */
@Service
public class OutboxEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    @Autowired(required = false)
    private PubSubTemplate pubSubTemplate;

    @Autowired(required = false)
    private OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        if (pubSubTemplate == null || outboxEventRepository == null) {
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findRetryableEvents();
        for (OutboxEvent event : pendingEvents) {
            try {
                pubSubTemplate.publish(event.getTopic(), event.getPayload()).get();
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.debug("Outbox event published: eventId={} topic={}", event.getEventId(), event.getTopic());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                    log.error("Outbox event permanently failed: eventId={} topic={}", event.getEventId(), event.getTopic(), e);
                } else {
                    log.warn("Outbox event publish failed, will retry: eventId={} topic={} attempt={}",
                             event.getEventId(), event.getTopic(), event.getRetryCount());
                }
                outboxEventRepository.save(event);
            }
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
}
