package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.models.ProcessedMessage;
import com.looksee.models.repository.ProcessedMessageRepository;

/**
 * Service providing idempotent message processing guarantees.
 *
 * <p>Before processing a PubSub message, call {@link #isAlreadyProcessed}
 * with the PubSub envelope's {@code messageId} and the service name.
 * After successful processing, call {@link #markProcessed} to record it.
 *
 * <p><b>Usage pattern:</b>
 * <pre>{@code
 * String pubsubMsgId = body.getMessage().getMessageId();
 * if (idempotencyService.isAlreadyProcessed(pubsubMsgId, "page-builder")) {
 *     return ResponseEntity.ok("Duplicate message, already processed");
 * }
 * // ... process message ...
 * idempotencyService.markProcessed(pubsubMsgId, "page-builder");
 * }</pre>
 *
 * <p>Processed message records are automatically cleaned up after 3 days
 * via a scheduled job (PubSub message retention is 7 days, so 3 days
 * provides adequate coverage with margin).
 */
@Service
public class IdempotencyService implements IdempotencyGuard {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int RETENTION_DAYS = 3;

    @Autowired(required = false)
    private ProcessedMessageRepository processedMessageRepository;

    /**
     * Checks if a PubSub message has already been processed by a service.
     *
     * @param pubsubMessageId the PubSub envelope messageId
     * @param serviceName the service checking for duplicates
     * @return true if the message was already processed
     */
    @Override
    public boolean isAlreadyProcessed(String pubsubMessageId, String serviceName) {
        if (processedMessageRepository == null || pubsubMessageId == null || pubsubMessageId.isEmpty()) {
            return false;
        }

        boolean exists = processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMessageId, serviceName);
        if (exists) {
            log.info("Duplicate message detected: pubsubMessageId={} service={}", pubsubMessageId, serviceName);
        }
        return exists;
    }

    /**
     * Records that a PubSub message has been successfully processed.
     *
     * @param pubsubMessageId the PubSub envelope messageId
     * @param serviceName the service that processed the message
     */
    @Override
    public void markProcessed(String pubsubMessageId, String serviceName) {
        if (processedMessageRepository == null || pubsubMessageId == null || pubsubMessageId.isEmpty()) {
            return;
        }

        try {
            ProcessedMessage record = new ProcessedMessage(pubsubMessageId, serviceName);
            processedMessageRepository.save(record);
            log.debug("Recorded processed message: pubsubMessageId={} service={}", pubsubMessageId, serviceName);
        } catch (Exception e) {
            log.warn("Failed to record processed message (non-fatal): pubsubMessageId={} service={}", pubsubMessageId, serviceName, e);
        }
    }

    /**
     * Atomically claims this {@code (pubsubMessageId, serviceName)} pair for processing.
     *
     * <p>Backed by a single Cypher {@code MERGE} against the {@code ProcessedMessage}
     * uniqueness constraint (migration V002). Two concurrent calls with the same key
     * are physically serialized by Neo4j; exactly one returns {@code true}.
     *
     * <p>Returns {@code true} when the caller should proceed with processing.
     * In the fail-open paths (null repository, null/empty messageId) returns
     * {@code true} as well so callers behave like before — same effect as the
     * legacy {@link #isAlreadyProcessed} returning {@code false}.
     */
    @Override
    public boolean claim(String pubsubMessageId, String serviceName) {
        if (processedMessageRepository == null || pubsubMessageId == null || pubsubMessageId.isEmpty()) {
            return true;
        }

        try {
            boolean firstClaim = processedMessageRepository.claim(pubsubMessageId, serviceName);
            if (!firstClaim) {
                log.info("Duplicate message detected via claim(): pubsubMessageId={} service={}", pubsubMessageId, serviceName);
            } else {
                log.debug("Claimed message: pubsubMessageId={} service={}", pubsubMessageId, serviceName);
            }
            return firstClaim;
        } catch (Exception e) {
            log.warn("claim() failed; falling back to allow processing (at-least-once): pubsubMessageId={} service={}",
                    pubsubMessageId, serviceName, e);
            return true;
        }
    }

    /**
     * Deletes a previously-claimed {@code (pubsubMessageId, serviceName)}
     * record so a subsequent Pub/Sub redelivery is allowed to re-run
     * business logic.
     *
     * <p>Best-effort: a missing repository, null/empty messageId, or a
     * thrown persistence exception are all logged and swallowed. A stuck
     * claim is preferable to bubbling a release-failure up over the
     * original handler error.
     */
    @Override
    public void release(String pubsubMessageId, String serviceName) {
        if (processedMessageRepository == null || pubsubMessageId == null || pubsubMessageId.isEmpty()) {
            return;
        }
        try {
            processedMessageRepository.release(pubsubMessageId, serviceName);
            log.debug("Released claim: pubsubMessageId={} service={}", pubsubMessageId, serviceName);
        } catch (Exception e) {
            log.warn("Failed to release claim (non-fatal); message may be stuck as duplicate: pubsubMessageId={} service={}",
                    pubsubMessageId, serviceName, e);
        }
    }

    /**
     * Cleans up old processed message records. Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecords() {
        if (processedMessageRepository == null) {
            return;
        }
        try {
            processedMessageRepository.deleteOlderThan(RETENTION_DAYS);
            log.info("Cleaned up processed message records older than {} days", RETENTION_DAYS);
        } catch (Exception e) {
            log.warn("Failed to clean up old processed message records", e);
        }
    }
}
