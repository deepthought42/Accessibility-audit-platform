package com.looksee.models.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.OutboxEvent;

@Repository
public interface OutboxEventRepository extends Neo4jRepository<OutboxEvent, Long> {

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PENDING' RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEvent> findPendingEvents();

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PENDING' AND e.retryCount < 5 RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEvent> findRetryableEvents();

    /**
     * Returns pending events that are due to be (re)published — i.e. either
     * have never been attempted or whose {@code nextAttemptAt} has elapsed.
     *
     * <p>Events without a {@code nextAttemptAt} (legacy rows from before this
     * field existed) are treated as due, so the publisher does not strand
     * them. New rows always get {@code nextAttemptAt = createdAt}.</p>
     */
    @Query("MATCH (e:OutboxEvent) "
         + "WHERE e.status = 'PENDING' AND e.retryCount < 5 "
         + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= $now) "
         + "RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEvent> findDueEvents(@Param("now") LocalDateTime now);

    /**
     * Returns the total number of due pending events. Backs the
     * {@code looksee.outbox.pending} gauge so it reflects the true backlog
     * rather than the limit-100 page size of {@link #findDueEvents}.
     */
    @Query("MATCH (e:OutboxEvent) "
         + "WHERE e.status = 'PENDING' AND e.retryCount < 5 "
         + "AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= $now) "
         + "RETURN count(e)")
    long countDueEvents(@Param("now") LocalDateTime now);

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PROCESSED' AND e.processedAt < datetime() - duration('P7D') DETACH DELETE e")
    void deleteOldProcessedEvents();

    /**
     * Sweeps terminally-failed events older than 30 days so the dormant
     * scaffold does not accumulate forever.
     */
    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'FAILED' AND e.processedAt < datetime() - duration('P30D') DETACH DELETE e")
    void deleteOldFailedEvents();
}
