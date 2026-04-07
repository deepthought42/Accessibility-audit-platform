package com.looksee.messaging.idempotency;

/**
 * Minimal contract for de-duplicating Pub/Sub messages by their envelope id.
 *
 * <p>Defined in {@code looksee-messaging} so the shared
 * {@link com.looksee.messaging.web.PubSubAuditController} can require an
 * idempotency check without forcing {@code looksee-messaging} to depend on
 * {@code looksee-persistence} (which would create a cycle, since persistence
 * already depends on messaging for trace-context propagation in
 * {@code OutboxEventPublisher}).</p>
 *
 * <p>The production implementation is
 * {@code com.looksee.services.IdempotencyService} in
 * {@code looksee-persistence}, which persists records to Neo4j. Tests can
 * use any in-memory implementation.</p>
 */
public interface IdempotencyGuard {

    /**
     * @return true if the given Pub/Sub messageId has already been processed
     *         by the named service. Implementations must be safe to call with
     *         a null or empty messageId (return false in that case so the
     *         caller can fall through to normal processing).
     */
    boolean isAlreadyProcessed(String pubsubMessageId, String serviceName);

    /**
     * Records that the given Pub/Sub messageId was successfully processed by
     * the named service. Implementations must tolerate null/empty inputs and
     * must not throw on persistence failures (de-duplication is best-effort).
     */
    void markProcessed(String pubsubMessageId, String serviceName);
}
