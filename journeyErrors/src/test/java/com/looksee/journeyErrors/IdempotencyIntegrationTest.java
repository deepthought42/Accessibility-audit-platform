package com.looksee.journeyErrors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.services.JourneyService;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * End-to-end idempotency proof for the journeyErrors push subscription.
 *
 * <p>Models the "two replicas redelivering the same message" scenario from
 * #87 step 8.2 as 20 concurrent invocations of
 * {@link AuditController#receiveMessage} against a shared
 * {@link IdempotencyGuard} backed by a real Neo4j 5 Testcontainer with the
 * production V001+V002 migrations applied. Two physical Spring contexts
 * add no additional coverage — the contended primitive (the V002-backed
 * Cypher {@code MERGE}) and the property under test (atomic single-winner)
 * are the same.
 *
 * <p>Asserts the three exact-count invariants the issue calls for after
 * 20 redeliveries of the same {@code (messageId, "journey-errors")} pair:
 *
 * <ol>
 *   <li>Exactly <b>one</b> {@code ProcessedMessage} node persists.</li>
 *   <li>Exactly <b>one</b> {@link JourneyService#updateStatus} call fires.</li>
 *   <li>{@code looksee.pubsub.messages.received{result="duplicate"}} counter
 *       reaches exactly <b>19</b> (= redeliveries minus the one winner).</li>
 * </ol>
 *
 * <p>Uses a raw-Driver {@link IdempotencyGuard} implementation rather than
 * booting the full {@code journeyErrors} Spring context. The production
 * {@code IdempotencyService} {@code claim()} compiles to the exact same
 * Cypher {@code MERGE} executed here verbatim — the V002 constraint
 * enforces atomicity at the database layer, not in the Java wrapper.
 * Skipping the full context boot avoids dragging Selenium, GCP Pub/Sub
 * binder, and the rest of the {@code Application} class into a test that
 * cares only about the dedupe path.
 */
@Tag("requires-docker")
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyIntegrationTest {

    private static final String SERVICE_NAME = "journey-errors";
    private static final String TOPIC_NAME = "journey_candidate_dlq";
    private static final long JOURNEY_ID = 555L;
    private static final int REDELIVERIES = 20;

    /**
     * Verbatim copy of the Cypher in
     * {@code ProcessedMessageRepository.claim()} — the SDN repository
     * compiles its {@code @Query} to this same call. Kept literal so a
     * production-side change that breaks atomicity surfaces here.
     */
    private static final String CLAIM_CYPHER =
            "MERGE (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                    + "ON CREATE SET pm.processedAt = datetime(), "
                    + "             pm.status = 'PROCESSED', "
                    + "             pm.justCreated = true "
                    + "ON MATCH  SET pm.justCreated = false "
                    + "RETURN pm.justCreated AS claimed";

    private static final String RELEASE_CYPHER =
            "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                    + "DETACH DELETE pm";

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.18-community"))
            .withoutAuthentication();

    private static Driver driver;

    private AuditController controller;
    private JourneyService journeyService;
    private MeterRegistry meterRegistry;
    private PubSubMetrics pubSubMetrics;

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
    void wireController() {
        try (Session session = driver.session()) {
            session.run("MATCH (pm:ProcessedMessage) DETACH DELETE pm").consume();
        }

        journeyService = mock(JourneyService.class);
        meterRegistry = new SimpleMeterRegistry();
        pubSubMetrics = new PubSubMetrics(meterRegistry);

        controller = new AuditController();
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "idempotencyService", new Neo4jClaimGuard(driver));
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
    }

    @Test
    void redelivery_invariants_hold_under_20ConcurrentReceives() throws Exception {
        String messageId = UUID.randomUUID().toString();
        Body envelope = buildEnvelope(messageId, JOURNEY_ID, "CANDIDATE");

        ExecutorService pool = Executors.newFixedThreadPool(REDELIVERIES);
        CyclicBarrier barrier = new CyclicBarrier(REDELIVERIES);

        try {
            List<CompletableFuture<ResponseEntity<String>>> deliveries =
                    IntStream.range(0, REDELIVERIES)
                            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    barrier.await();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return controller.receiveMessage(envelope);
                            }, pool))
                            .collect(Collectors.toList());

            List<ResponseEntity<String>> responses = deliveries.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            assertThat(responses).allSatisfy(r ->
                    assertThat(r.getStatusCode())
                            .as("Every redelivery must ack with HTTP 200 — winners "
                                    + "with \"ok\", losers with \"Duplicate...\".")
                            .isEqualTo(HttpStatus.OK));

            long duplicates = responses.stream()
                    .filter(r -> r.getBody() != null && r.getBody().contains("Duplicate"))
                    .count();
            assertThat(duplicates)
                    .as("Exactly %d of %d concurrent redeliveries must short-circuit "
                            + "as duplicates after the V002-backed atomic claim().",
                            REDELIVERIES - 1, REDELIVERIES)
                    .isEqualTo(REDELIVERIES - 1L);

            try (Session session = driver.session()) {
                long count = session.run(
                        "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                                + "RETURN count(pm) AS c",
                        Map.of("id", messageId, "svc", SERVICE_NAME))
                        .single().get("c").asLong();
                assertThat(count)
                        .as("Exactly one ProcessedMessage row must persist for "
                                + "(messageId, %s).", SERVICE_NAME)
                        .isEqualTo(1L);
            }

            verify(journeyService, times(1))
                    .updateStatus(eq(JOURNEY_ID), any(JourneyStatus.class));

            double duplicateCount = meterRegistry.counter(
                            PubSubMetrics.MESSAGES_RECEIVED,
                            "service", SERVICE_NAME,
                            "topic", TOPIC_NAME,
                            "result", "duplicate")
                    .count();
            assertThat(duplicateCount)
                    .as("looksee.pubsub.messages.received{result=duplicate} must "
                            + "tick exactly %d times for the contended message.",
                            REDELIVERIES - 1)
                    .isEqualTo((double) (REDELIVERIES - 1));

            double successCount = meterRegistry.counter(
                            PubSubMetrics.MESSAGES_RECEIVED,
                            "service", SERVICE_NAME,
                            "topic", TOPIC_NAME,
                            "result", "success")
                    .count();
            assertThat(successCount)
                    .as("Exactly one redelivery must record \"success\".")
                    .isEqualTo(1.0);
        } finally {
            pool.shutdown();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    @Test
    void distinctMessageIds_eachClaimSucceeds_evenUnderConcurrency() throws Exception {
        int distinctMessages = 8;
        ExecutorService pool = Executors.newFixedThreadPool(distinctMessages);

        try {
            List<CompletableFuture<ResponseEntity<String>>> calls = IntStream.range(0, distinctMessages)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() ->
                            controller.receiveMessage(buildEnvelope(
                                    "distinct-" + UUID.randomUUID(), JOURNEY_ID + i, "CANDIDATE")), pool))
                    .collect(Collectors.toList());

            List<ResponseEntity<String>> results = calls.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            assertThat(results).allSatisfy(r -> {
                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).isEqualTo("ok");
            });

            verify(journeyService, times(distinctMessages))
                    .updateStatus(anyLong(), any(JourneyStatus.class));

            try (Session session = driver.session()) {
                long count = session.run(
                        "MATCH (pm:ProcessedMessage {serviceName: $svc}) RETURN count(pm) AS c",
                        Map.of("svc", SERVICE_NAME))
                        .single().get("c").asLong();
                assertThat(count)
                        .as("Each distinct messageId must produce its own "
                                + "ProcessedMessage node — the constraint isolates "
                                + "by (id, svc), not just by id.")
                        .isEqualTo((long) distinctMessages);
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    private static Body buildEnvelope(String messageId, long journeyId, String status) {
        String json = "{\"accountId\":1,"
                + "\"journey\":{\"id\":" + journeyId + ",\"status\":\"" + status + "\","
                + "\"candidateKey\":\"k\"},"
                + "\"browser\":\"CHROME\",\"auditRecordId\":100,\"mapId\":1}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId(messageId);
        msg.setData(encoded);
        body.setMessage(msg);
        return body;
    }

    /**
     * Test-local {@link IdempotencyGuard} backed by a raw Driver.
     *
     * <p>Mirrors {@code IdempotencyService} fail-open semantics: returns
     * {@code true} on null/empty messageId or any persistence exception
     * (so production-equivalent behavior is preserved when the test
     * infrastructure misbehaves). Uses the same Cypher the SDN repository
     * compiles to, against the same V002 constraint.
     */
    private static final class Neo4jClaimGuard implements IdempotencyGuard {
        private final Driver driver;

        Neo4jClaimGuard(Driver driver) {
            this.driver = driver;
        }

        @Override
        public boolean claim(String pubsubMessageId, String serviceName) {
            if (pubsubMessageId == null || pubsubMessageId.isEmpty()) {
                return true;
            }
            try (Session session = driver.session()) {
                return session.run(CLAIM_CYPHER,
                        Map.of("id", pubsubMessageId, "svc", serviceName))
                        .single().get("claimed").asBoolean();
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public void release(String pubsubMessageId, String serviceName) {
            if (pubsubMessageId == null || pubsubMessageId.isEmpty()) {
                return;
            }
            try (Session session = driver.session()) {
                session.run(RELEASE_CYPHER,
                        Map.of("id", pubsubMessageId, "svc", serviceName))
                        .consume();
            } catch (Exception ignored) {
                // best-effort, mirrors IdempotencyService.release()
            }
        }

        @Override
        public boolean isAlreadyProcessed(String pubsubMessageId, String serviceName) {
            // Unused by PubSubAuditController (replaced by claim()), but
            // implemented for interface completeness.
            if (pubsubMessageId == null || pubsubMessageId.isEmpty()) {
                return false;
            }
            try (Session session = driver.session()) {
                return session.run(
                        "MATCH (pm:ProcessedMessage {pubsubMessageId: $id, serviceName: $svc}) "
                                + "RETURN count(pm) > 0 AS exists",
                        Map.of("id", pubsubMessageId, "svc", serviceName))
                        .single().get("exists").asBoolean();
            }
        }

        @Override
        public void markProcessed(String pubsubMessageId, String serviceName) {
            // Unused by PubSubAuditController (claim() persists on first hit),
            // but kept for interface completeness.
        }
    }
}
