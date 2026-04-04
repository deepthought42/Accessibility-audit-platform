package com.looksee.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.looksee.models.OutboxEvent;

@Repository
public interface OutboxEventRepository extends Neo4jRepository<OutboxEvent, Long> {

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PENDING' RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEvent> findPendingEvents();

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PENDING' AND e.retryCount < 5 RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEvent> findRetryableEvents();

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PROCESSED' AND e.processedAt < datetime() - duration('P7D') DETACH DELETE e")
    void deleteOldProcessedEvents();
}
