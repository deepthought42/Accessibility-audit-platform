package com.looksee.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

/**
 * End-to-end rehearsal of the V001 + V002 Cypher migrations against a
 * real Neo4j 5 container.
 *
 * <p>Pre-seeds duplicate {@code ProcessedMessage} nodes with the same
 * {@code (pubsubMessageId, serviceName)} pair — the exact shape the
 * legacy TOCTOU race produced — then runs {@link Migrations#apply()}
 * via the same starter the production app uses on boot. Asserts:
 *
 * <ul>
 *   <li>V001 deduplicates the seeded collisions to exactly one node
 *       per {@code (id, svc)} key.</li>
 *   <li>V002 creates the {@code processed_message_unique} uniqueness
 *       constraint (visible via {@code SHOW CONSTRAINTS}).</li>
 *   <li>A subsequent attempt to insert another {@code ProcessedMessage}
 *       with the same key is rejected by Neo4j with
 *       {@code Neo.ClientError.Schema.ConstraintValidationFailed} —
 *       the live invariant the atomic {@code claim()} relies on.</li>
 * </ul>
 *
 * <p>Doubles as the production-snapshot rehearsal called for in
 * #89.10.2 — the snapshot is synthesized rather than imported, which
 * is sufficient to prove migration correctness against pre-existing
 * collisions.
 *
 * <p>Companion to {@link MigrationsResolveTest}, which only verifies
 * the migration files resolve on the classpath.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigrationsApplyIntegrationTest {

    private static final String DUPLICATE_MESSAGE_ID = "dup-message-1";
    private static final String SERVICE_NAME = "rehearsal-svc";
    private static final String CONSTRAINT_NAME = "processed_message_unique";

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.18-community"))
            .withoutAuthentication();

    private static Driver driver;

    @BeforeAll
    static void connect() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl());
        seedDuplicateProcessedMessages();
        applyMigrations();
    }

    @AfterAll
    static void close() {
        if (driver != null) {
            driver.close();
        }
    }

    private static void seedDuplicateProcessedMessages() {
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc, "
                            + "status: 'PROCESSED', processedAt: datetime()}) "
                            + "CREATE (:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc, "
                            + "status: 'PROCESSED', processedAt: datetime()}) "
                            + "CREATE (:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc, "
                            + "status: 'PROCESSED', processedAt: datetime()})",
                    Map.of("id", DUPLICATE_MESSAGE_ID, "svc", SERVICE_NAME))
                    .consume();
        }
    }

    private static void applyMigrations() {
        Migrations migrations = new Migrations(
                MigrationsConfig.builder()
                        .withLocationsToScan("classpath:neo4j/migrations")
                        .build(),
                driver);
        migrations.apply();
    }

    @Test
    void v001_deduplicates_preExistingCollisions_toExactlyOneNode() {
        try (Session session = driver.session()) {
            long count = session.run(
                    "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                            + "RETURN count(pm) AS c",
                    Map.of("id", DUPLICATE_MESSAGE_ID, "svc", SERVICE_NAME))
                    .single().get("c").asLong();
            assertThat(count)
                    .as("V001 must collapse the 3 seeded duplicates to 1 node "
                            + "(otherwise V002's constraint creation would fail).")
                    .isEqualTo(1L);
        }
    }

    @Test
    void v002_createsTheProcessedMessageUniquenessConstraint() {
        try (Session session = driver.session()) {
            List<Record> constraints = session.run("SHOW CONSTRAINTS").list();
            boolean present = constraints.stream()
                    .anyMatch(r -> CONSTRAINT_NAME.equals(r.get("name").asString()));
            assertThat(present)
                    .as("V002 must create the %s constraint; SHOW CONSTRAINTS returned: %s",
                            CONSTRAINT_NAME,
                            constraints.stream()
                                    .map(r -> r.get("name").asString())
                                    .toList())
                    .isTrue();
        }
    }

    @Test
    void v002_constraint_rejectsSubsequentDuplicateInserts() {
        String key = "post-migration-dup-id";
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc, "
                            + "status: 'PROCESSED', processedAt: datetime()})",
                    Map.of("id", key, "svc", SERVICE_NAME))
                    .consume();

            assertThatThrownBy(() -> session.run(
                    "CREATE (:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc, "
                            + "status: 'PROCESSED', processedAt: datetime()})",
                    Map.of("id", key, "svc", SERVICE_NAME))
                    .consume())
                    .as("Neo4j must reject a duplicate (id, svc) insert with "
                            + "ConstraintValidationFailed — this is the invariant the "
                            + "atomic claim() relies on.")
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("ConstraintValidationFailed");
        }
    }
}
