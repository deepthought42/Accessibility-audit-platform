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
}
