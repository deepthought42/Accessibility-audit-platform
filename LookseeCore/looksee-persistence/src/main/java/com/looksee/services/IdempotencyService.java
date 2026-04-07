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
