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

    /**
     * Atomically claims the given Pub/Sub messageId for the named service.
     *
     * <p>Returns {@code true} iff this caller is the first to claim it; subsequent
     * calls with the same {@code (pubsubMessageId, serviceName)} pair return
     * {@code false}. This collapses the legacy
     * {@link #isAlreadyProcessed}/{@link #markProcessed} pair into a single
     * operation, eliminating the TOCTOU window where two concurrent deliveries
     * both see "not processed" and both proceed.
     *
     * <p>Implementations that cannot dedupe (e.g. repository unavailable, or
     * null/empty messageId) must return {@code true} so the caller falls
     * through to normal processing — same fail-open semantics as the legacy
     * pair, just expressed in the opposite direction.
     *
     * <p>The default implementation here delegates to the legacy pair so any
     * existing {@link IdempotencyGuard} implementor keeps compiling. The
     * production implementation in {@code IdempotencyService} overrides this
     * with a single Cypher {@code MERGE} backed by the Neo4j uniqueness
     * constraint on {@code (pubsubMessageId, serviceName)}.
     */
    default boolean claim(String pubsubMessageId, String serviceName) {
        if (isAlreadyProcessed(pubsubMessageId, serviceName)) {
            return false;
        }
        markProcessed(pubsubMessageId, serviceName);
        return true;
    }
}
