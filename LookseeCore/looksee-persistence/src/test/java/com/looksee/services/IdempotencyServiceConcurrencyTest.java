package com.looksee.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.looksee.config.LookseeCoreRepositoryConfiguration;
import com.looksee.models.ProcessedMessage;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;

/**
 * Concurrency proof for the atomic-claim invariant added in #82 / #91.
 *
 * <p>100 threads contend on {@link IdempotencyService#claim(String, String)}
 * with the same {@code (pubsubMessageId, serviceName)}; exactly one must
 * receive {@code true} and exactly one {@code ProcessedMessage} node must
 * exist in Neo4j after the storm.
 *
 * <p>This is the test referenced by issue #87 step 8.1 and by
 * {@code IdempotencyService}'s own Javadoc — without the V002 uniqueness
 * constraint the Cypher {@code MERGE} is not serialized and multiple
 * winners appear. The same test, run before the constraint exists, would
 * fail; with the constraint in place it must pass.
 *
 * <p>Bootstraps a real Neo4j 5 via Testcontainers and runs the V001+V002
 * migrations through the production {@link Migrations} API so the test
 * reflects the same state production starts up in.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataNeo4jTest
@EntityScan(basePackageClasses = ProcessedMessage.class)
@Import({LookseeCoreRepositoryConfiguration.class, IdempotencyService.class})
class IdempotencyServiceConcurrencyTest {

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.18-community"))
            .withoutAuthentication();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", NEO4J::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "neo4j");
    }

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private Driver driver;

    @BeforeAll
    static void applyMigrationsOnce() {
        try (Driver bootDriver = org.neo4j.driver.GraphDatabase.driver(NEO4J.getBoltUrl())) {
            Migrations migrations = new Migrations(
                    MigrationsConfig.builder()
                            .withLocationsToScan("classpath:neo4j/migrations")
                            .build(),
                    bootDriver);
            migrations.apply();
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
                        return idempotencyService.claim(messageId, serviceName);
                    }, pool))
                    .collect(Collectors.toList());

            List<Boolean> results = claims.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            long winners = results.stream().filter(Boolean::booleanValue).count();
            assertThat(winners)
                    .as("Exactly one of %d concurrent claim() calls must win; "
                            + "the V002 uniqueness constraint serializes the MERGE.",
                            threadCount)
                    .isEqualTo(1L);

            try (Session session = driver.session()) {
                long nodeCount = session.run(
                        "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                                + "RETURN count(pm) AS c",
                        java.util.Map.of("id", messageId, "svc", serviceName))
                        .single().get("c").asLong();
                assertThat(nodeCount)
                        .as("Exactly one ProcessedMessage node must exist for the "
                                + "contended (id, svc) key after the storm.")
                        .isEqualTo(1L);
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void claim_isolatesByServiceName() {
        String messageId = UUID.randomUUID().toString();

        assertThat(idempotencyService.claim(messageId, "service-a"))
                .as("First claim for (id, service-a) should win.")
                .isTrue();
        assertThat(idempotencyService.claim(messageId, "service-b"))
                .as("Same id but different serviceName must not collide; "
                        + "the V002 constraint is on the (id, service) tuple.")
                .isTrue();
        assertThat(idempotencyService.claim(messageId, "service-a"))
                .as("Re-claim of (id, service-a) must return false (duplicate).")
                .isFalse();
    }
}
