package com.looksee.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Asserts that the Cypher migration files under
 * {@code src/main/resources/neo4j/migrations/} are present on the classpath
 * with the names neo4j-migrations expects, and contain non-empty Cypher.
 *
 * <p>Verifies the resource-resolution surface that the
 * {@code neo4j-migrations-spring-boot-starter} relies on at runtime,
 * without standing up a Neo4j instance. End-to-end migration application
 * is verified manually against a local Neo4j (see issue #80 closing
 * comment) and will be covered by Testcontainers integration in #87.
 */
class MigrationsResolveTest {

    private static final String MIGRATIONS_BASE = "/neo4j/migrations/";

    @Test
    void v001_dedupeProcessedMessage_isOnClasspath_andHasCypher() throws IOException {
        assertCypherMigration("V001__dedupe_processed_message.cypher", "DETACH DELETE");
    }

    @Test
    void v002_processedMessageUnique_isOnClasspath_andHasCypher() throws IOException {
        assertCypherMigration("V002__processed_message_unique.cypher", "CREATE CONSTRAINT");
    }

    private void assertCypherMigration(String filename, String expectedFragment) throws IOException {
        String resource = MIGRATIONS_BASE + filename;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertThat(in)
                .as("classpath resource %s must exist; neo4j-migrations resolves "
                        + "migrations by classpath scanning at runtime", resource)
                .isNotNull();
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content)
                .as("%s must contain the expected Cypher fragment", filename)
                .contains(expectedFragment);
            assertThat(content.trim())
                .as("%s must end with a semicolon (statement separator)", filename)
                .endsWith(";");
        }
    }
}
