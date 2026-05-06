package com.looksee.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.ProcessedMessage;

/**
 * Repository for tracking processed PubSub messages to enable idempotent
 * message handling across all services.
 */
@Repository
public interface ProcessedMessageRepository extends Neo4jRepository<ProcessedMessage, Long> {

    /**
     * Checks whether a message has already been processed by a specific service.
     *
     * @param pubsubMessageId the PubSub envelope message ID
     * @param serviceName the name of the service
     * @return true if the message was already processed
     */
    @Query("MATCH (pm:ProcessedMessage {pubsubMessageId: $pubsubMessageId, serviceName: $serviceName}) RETURN COUNT(pm) > 0")
    boolean existsByPubsubMessageIdAndServiceName(
        @Param("pubsubMessageId") String pubsubMessageId,
        @Param("serviceName") String serviceName);

    /**
     * Removes processed message records older than the specified number of days.
     *
     * @param days number of days to retain
     */
    @Query("MATCH (pm:ProcessedMessage) WHERE pm.processedAt < datetime() - duration({days: $days}) DETACH DELETE pm")
    void deleteOlderThan(@Param("days") int days);

    /**
     * Atomically claims a {@code (pubsubMessageId, serviceName)} pair via a
     * single Cypher {@code MERGE}. Returns {@code true} iff this call created
     * the node; {@code false} if it already existed.
     *
     * <p>Backed by the {@code processed_message_unique} constraint
     * (migration {@code V002}) so two concurrent claims for the same key are
     * physically serialized by Neo4j — there is no TOCTOU window the way
     * {@link #existsByPubsubMessageIdAndServiceName} + {@code save(...)} had.
     *
     * @param pubsubMessageId the PubSub envelope message ID
     * @param serviceName the name of the service claiming the message
     * @return true if this call created the record, false if it already existed
     */
    @Query("""
        MERGE (pm:ProcessedMessage {pubsubMessageId: $pubsubMessageId, serviceName: $serviceName})
        ON CREATE SET pm.processedAt = datetime(), pm.status = 'PROCESSED', pm.justCreated = true
        ON MATCH  SET pm.justCreated = false
        RETURN pm.justCreated AS claimed
        """)
    boolean claim(
        @Param("pubsubMessageId") String pubsubMessageId,
        @Param("serviceName") String serviceName);

    /**
     * Releases a previously-claimed {@code (pubsubMessageId, serviceName)}
     * pair by deleting its {@code ProcessedMessage} node, so a subsequent
     * Pub/Sub redelivery is allowed to re-run business logic.
     *
     * <p>Called from {@code IdempotencyService.release(...)} when a
     * controller fails inside {@code handle(...)} and returns 500. The
     * uniqueness constraint guarantees there is at most one node to remove.
     */
    @Query("""
        MATCH (pm:ProcessedMessage {pubsubMessageId: $pubsubMessageId, serviceName: $serviceName})
        DETACH DELETE pm
        """)
    void release(
        @Param("pubsubMessageId") String pubsubMessageId,
        @Param("serviceName") String serviceName);
}
