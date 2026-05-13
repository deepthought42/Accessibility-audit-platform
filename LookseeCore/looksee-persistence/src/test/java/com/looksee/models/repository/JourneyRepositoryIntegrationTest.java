package com.looksee.models.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.driver.Values.parameters;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Regression test for {@link JourneyRepository#changeJourneyStatus} (#103).
 *
 * <p>The repository method previously hardcoded the status transition
 * (CANDIDATE → ERROR) in its Cypher despite declaring {@code @Param}
 * values, so any caller passing a different transition was silently
 * routed to the hardcoded one. The fix parameterizes the literals as
 * {@code $status} / {@code $goal_status}; this test exercises the
 * production Cypher against a real Neo4j 5 container to lock in
 * correct parameter binding.
 *
 * <p>Pattern mirrors {@link com.looksee.migrations.MigrationsApplyIntegrationTest}:
 * raw {@link Driver} against a {@link Neo4jContainer}, skipped cleanly
 * when Docker is unavailable. Spring Data Neo4j wiring is not exercised
 * because the bug lives entirely in the {@code @Query} string — the
 * driver runs the same Cypher Spring Data would send.
 */
@Tag("requires-docker")
@Testcontainers(disabledWithoutDocker = true)
class JourneyRepositoryIntegrationTest {

    /**
     * Production Cypher copied verbatim from
     * {@code JourneyRepository#changeJourneyStatus}. Kept in sync by
     * convention; the three transition tests below would all fail if
     * the production query drifts in a meaningful way.
     */
    private static final String CHANGE_STATUS_CYPHER =
            "MATCH (map:DomainMap)-[]->(journey:Journey) "
            + "WHERE id(map)=$map_id AND journey.status=$status "
            + "SET journey.status=$goal_status RETURN journey";

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.18-community"))
            .withoutAuthentication();

    private static Driver driver;

    @BeforeAll
    static void connect() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl());
    }

    @AfterAll
    static void close() {
        if (driver != null) {
            driver.close();
        }
    }

    @BeforeEach
    void clearGraph() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    void changesCandidateToErrorTransition() {
        long mapId = seedMapWithJourney("CANDIDATE");

        runChangeStatus(mapId, "CANDIDATE", "ERROR");

        assertThat(journeyStatus(mapId)).isEqualTo("ERROR");
    }

    /**
     * Regression for the latent bug: prior to the fix, the Cypher
     * hardcoded {@code WHERE journey.status="CANDIDATE"} and
     * {@code SET journey.status="ERROR"}, so this transition would
     * leave a REVIEWING journey untouched and would never produce
     * DISCARDED.
     */
    @Test
    void changesReviewingToDiscardedTransition() {
        long mapId = seedMapWithJourney("REVIEWING");

        runChangeStatus(mapId, "REVIEWING", "DISCARDED");

        assertThat(journeyStatus(mapId)).isEqualTo("DISCARDED");
    }

    @Test
    void doesNotTouchJourneysWithOtherStatus() {
        long mapId;
        try (Session session = driver.session()) {
            Record rec = session.run(
                    "CREATE (m:DomainMap) "
                    + "CREATE (j1:Journey {status:'CANDIDATE'}) "
                    + "CREATE (j2:Journey {status:'VERIFIED'}) "
                    + "CREATE (m)-[:CONTAINS]->(j1) "
                    + "CREATE (m)-[:CONTAINS]->(j2) "
                    + "RETURN id(m) AS mapId").single();
            mapId = rec.get("mapId").asLong();
        }

        runChangeStatus(mapId, "CANDIDATE", "ERROR");

        try (Session session = driver.session()) {
            Record statuses = session.run(
                    "MATCH (m:DomainMap)-[]->(j:Journey) WHERE id(m)=$map_id "
                    + "RETURN collect(j.status) AS statuses",
                    parameters("map_id", mapId)).single();
            assertThat(statuses.get("statuses").asList())
                    .containsExactlyInAnyOrder("ERROR", "VERIFIED");
        }
    }

    private long seedMapWithJourney(String initialStatus) {
        try (Session session = driver.session()) {
            Record rec = session.run(
                    "CREATE (m:DomainMap) "
                    + "CREATE (j:Journey {status:$status}) "
                    + "CREATE (m)-[:CONTAINS]->(j) "
                    + "RETURN id(m) AS mapId",
                    parameters("status", initialStatus)).single();
            return rec.get("mapId").asLong();
        }
    }

    private void runChangeStatus(long mapId, String status, String goalStatus) {
        try (Session session = driver.session()) {
            session.run(CHANGE_STATUS_CYPHER,
                    parameters("map_id", mapId, "status", status, "goal_status", goalStatus))
                    .consume();
        }
    }

    private String journeyStatus(long mapId) {
        try (Session session = driver.session()) {
            Record rec = session.run(
                    "MATCH (m:DomainMap)-[]->(j:Journey) WHERE id(m)=$map_id RETURN j.status AS status",
                    parameters("map_id", mapId)).single();
            return rec.get("status").asString();
        }
    }
}
