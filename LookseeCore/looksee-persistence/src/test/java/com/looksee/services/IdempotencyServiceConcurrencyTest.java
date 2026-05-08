package com.looksee.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

/**
 * Concurrency proof for the atomic-claim invariant added in #82 / #91.
 *
 * <p>100 threads contend on the exact same Cypher {@code MERGE} that
 * {@link com.looksee.models.repository.ProcessedMessageRepository#claim}
 * compiles to; exactly one must observe {@code justCreated = true} and
 * exactly one {@code ProcessedMessage} node must exist in Neo4j after the
 * storm.
 *
 * <p>This is the regression contract for issue #82 / PR #91. Without the
 * V002 uniqueness constraint the {@code MERGE} is not serialized and
 * multiple winners appear; with the constraint Neo4j physically serializes
 * concurrent writers and exactly one node is created.
 *
 * <p>The test deliberately exercises the raw Cypher rather than the
 * Spring-wired {@link IdempotencyService} bean: the atomicity property
 * lives at the database layer, not in the Java wrapper. Going through the
 * Driver instead of {@code @DataNeo4jTest} also means no Spring context is
 * required (the {@code looksee-persistence} module has no
 * {@code @SpringBootApplication} of its own), keeping the test
 * self-contained and immune to slice-test bootstrap fragility.
 *
 * <p>{@code IdempotencyServiceTest} continues to cover the wrapper's
 * fail-open / null-check semantics with Mockito.
 */
@Tag("requires-docker")
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyServiceConcurrencyTest {

    /**
     * The exact Cypher {@code MERGE} from
     * {@code ProcessedMessageRepository.claim()}. Kept verbatim so a
     * change to the production query that breaks atomicity will surface
     * here as a hard failure rather than passing silently.
     */
    private static final String CLAIM_CYPHER =
            "MERGE (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                    + "ON CREATE SET pm.processedAt = datetime(), "
                    + "             pm.status = 'PROCESSED', "
                    + "             pm.justCreated = true "
                    + "ON MATCH  SET pm.justCreated = false "
                    + "RETURN pm.justCreated AS claimed";

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.18-community"))
            .withoutAuthentication();

    private static Driver driver;

    @BeforeAll
    static void connectAndMigrate() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl());
        Migrations migrations = new Migrations(
                MigrationsConfig.builder()
                        .withLocationsToScan("classpath:neo4j/migrations")
                        .build(),
                driver);
        migrations.apply();
    }

    @AfterAll
    static void close() {
        if (driver != null) {
            driver.close();
        }
    }

    @BeforeEach
    void clearProcessedMessages() {
        try (Session session = driver.session()) {
            session.run("MATCH (pm:ProcessedMessage) DETACH DELETE pm").consume();
        }
    }

    @Test
    void claim_isAtomic_under_100ThreadContention() throws Exception {
        String messageId = UUID.randomUUID().toString();
        String serviceName = "concurrency-test";
        int threadCount = 100;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        try {
            List<CompletableFuture<Boolean>> claims = IntStream.range(0, threadCount)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            barrier.await();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        try (Session session = driver.session()) {
                            return session.run(CLAIM_CYPHER,
                                    Map.of("id", messageId, "svc", serviceName))
                                    .single().get("claimed").asBoolean();
                        }
                    }, pool))
                    .collect(Collectors.toList());

            List<Boolean> results = claims.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            long winners = results.stream().filter(Boolean::booleanValue).count();
            assertThat(winners)
                    .as("Exactly one of %d concurrent claims must win; "
                            + "the V002 uniqueness constraint serializes the MERGE.",
                            threadCount)
                    .isEqualTo(1L);

            try (Session session = driver.session()) {
                long nodeCount = session.run(
                        "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                                + "RETURN count(pm) AS c",
                        Map.of("id", messageId, "svc", serviceName))
                        .single().get("c").asLong();
                assertThat(nodeCount)
                        .as("Exactly one ProcessedMessage node must exist for the "
                                + "contended (id, svc) key after the storm.")
                        .isEqualTo(1L);
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    @Test
    void claim_isolatesByServiceName() {
        String messageId = UUID.randomUUID().toString();

        try (Session session = driver.session()) {
            boolean firstA = session.run(CLAIM_CYPHER,
                    Map.of("id", messageId, "svc", "service-a"))
                    .single().get("claimed").asBoolean();
            assertThat(firstA)
                    .as("First claim for (id, service-a) should win.")
                    .isTrue();

            boolean firstB = session.run(CLAIM_CYPHER,
                    Map.of("id", messageId, "svc", "service-b"))
                    .single().get("claimed").asBoolean();
            assertThat(firstB)
                    .as("Same id but different serviceName must not collide; "
                            + "the V002 constraint is on the (id, service) tuple.")
                    .isTrue();

            boolean secondA = session.run(CLAIM_CYPHER,
                    Map.of("id", messageId, "svc", "service-a"))
                    .single().get("claimed").asBoolean();
            assertThat(secondA)
                    .as("Re-claim of (id, service-a) must return false (duplicate).")
                    .isFalse();
        }
    }
}
