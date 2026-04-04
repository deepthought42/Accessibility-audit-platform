package com.looksee.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes Neo4j database constraints on application startup.
 *
 * These constraints prevent duplicate records caused by TOCTOU race conditions
 * in concurrent message processing. For example, two concurrent PageBuiltMessages
 * for the same page could both pass the "wasPageAlreadyAudited" check before
 * either creates a record, resulting in duplicate audit records.
 *
 * <p><b>Constraints created:</b>
 * <ul>
 *   <li>{@code PageAuditRecord.key} — unique constraint to prevent duplicate page audits</li>
 *   <li>{@code DomainAuditRecord.key} — unique constraint to prevent duplicate domain audits</li>
 *   <li>{@code Journey.candidateKey} — unique constraint to prevent duplicate journey candidates</li>
 *   <li>{@code OutboxEvent.eventId} — unique constraint for outbox event deduplication</li>
 *   <li>{@code ProcessedMessage(pubsubMessageId, serviceName)} — composite unique for message deduplication</li>
 * </ul>
 */
@Component
public class Neo4jConstraintInitializer {
    private static final Logger log = LoggerFactory.getLogger(Neo4jConstraintInitializer.class);

    @Autowired(required = false)
    private Driver neo4jDriver;

    @EventListener(ApplicationReadyEvent.class)
    public void createConstraints() {
        if (neo4jDriver == null) {
            log.warn("Neo4j driver not available; skipping constraint initialization");
            return;
        }

        String[] constraints = {
            "CREATE CONSTRAINT page_audit_record_key_unique IF NOT EXISTS FOR (p:PageAuditRecord) REQUIRE p.key IS UNIQUE",
            "CREATE CONSTRAINT domain_audit_record_key_unique IF NOT EXISTS FOR (d:DomainAuditRecord) REQUIRE d.key IS UNIQUE",
            "CREATE CONSTRAINT journey_candidate_key_unique IF NOT EXISTS FOR (j:Journey) REQUIRE j.candidateKey IS UNIQUE",
            "CREATE CONSTRAINT outbox_event_id_unique IF NOT EXISTS FOR (o:OutboxEvent) REQUIRE o.eventId IS UNIQUE",
            "CREATE CONSTRAINT processed_message_unique IF NOT EXISTS FOR (pm:ProcessedMessage) REQUIRE (pm.pubsubMessageId, pm.serviceName) IS UNIQUE"
        };

        try (Session session = neo4jDriver.session()) {
            for (String constraint : constraints) {
                try {
                    session.run(constraint);
                    log.info("Applied constraint: {}", constraint);
                } catch (Exception e) {
                    log.warn("Could not apply constraint: {} — {}", constraint, e.getMessage());
                }
            }
        }
        log.info("Neo4j constraint initialization complete");
    }
}
