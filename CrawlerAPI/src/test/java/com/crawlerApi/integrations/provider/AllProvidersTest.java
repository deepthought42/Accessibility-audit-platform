package com.crawlerApi.integrations.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationProvider;

class AllProvidersTest {

    static List<Arguments> providerTestCases() {
        return Arrays.asList(
            Arguments.of(new FigmaIntegrationProvider(), "figma", "Figma", "Design tools", Arrays.asList("token", "fileIds")),
            Arguments.of(new GithubIntegrationProvider(), "github", "Github", "CI/CD", Arrays.asList("token", "org", "repo")),
            Arguments.of(new GitLabIntegrationProvider(), "gitlab", "GitLab", "CI/CD", Arrays.asList("baseUrl", "token", "projectIds")),
            Arguments.of(new JenkinsIntegrationProvider(), "jenkins", "Jenkins", "CI/CD", Arrays.asList("baseUrl", "token", "jobNames")),
            Arguments.of(new JiraIntegrationProvider(), "jira", "Jira", "Product Management", Arrays.asList("baseUrl", "token", "projectKeys")),
            Arguments.of(new SlackIntegrationProvider(), "slack", "Slack", "Messaging", Arrays.asList("webhookUrl", "channel")),
            Arguments.of(new TrelloIntegrationProvider(), "trello", "Trello", "Product Management", Arrays.asList("apiKey", "token", "boardIds")),
            Arguments.of(new GoogleDriveIntegrationProvider(), "google-drive", "Google Drive", "Product Management", Arrays.asList("clientId", "clientSecret", "folderIds"))
        );
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderType(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        assertEquals(expectedType, provider.getType());
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderMetadata(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        IntegrationMetadata metadata = provider.getMetadata();
        assertEquals(expectedType, metadata.getId());
        assertEquals(expectedName, metadata.getName());
        assertEquals(expectedCategory, metadata.getCategory());
        assertEquals(expectedKeys, metadata.getConfigSchema());
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderValidateConfigWithAllKeys(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        Map<String, Object> config = new HashMap<>();
        for (String key : expectedKeys) {
            config.put(key, "test-value");
        }
        assertTrue(provider.validateConfig(config));
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderValidateConfigWithMissingKeys(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        if (expectedKeys.isEmpty()) return;
        Map<String, Object> config = new HashMap<>();
        // Only add first key, skip rest
        config.put(expectedKeys.get(0), "test-value");
        if (expectedKeys.size() > 1) {
            assertFalse(provider.validateConfig(config));
        }
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderValidateConfigNull(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        assertFalse(provider.validateConfig(null));
    }

    @ParameterizedTest
    @MethodSource("providerTestCases")
    void testProviderImplementsInterface(AbstractIntegrationProvider provider, String expectedType, String expectedName, String expectedCategory, List<String> expectedKeys) {
        assertInstanceOf(IntegrationProvider.class, provider);
    }

    // ProductBoard is special (implements IntegrationProvider directly, not AbstractIntegrationProvider)
    @Test
    void testProductBoardProvider() {
        ProductBoardIntegrationProvider provider = new ProductBoardIntegrationProvider();
        assertEquals("product-board", provider.getType());

        IntegrationMetadata metadata = provider.getMetadata();
        assertEquals("product-board", metadata.getId());
        assertEquals("Product Board", metadata.getName());
        assertEquals("Product Management", metadata.getCategory());
        assertTrue(metadata.getConfigSchema().isEmpty());
    }

    @Test
    void testProductBoardValidateConfigAlwaysTrue() {
        ProductBoardIntegrationProvider provider = new ProductBoardIntegrationProvider();
        assertTrue(provider.validateConfig(null));
        assertTrue(provider.validateConfig(new HashMap<>()));
        Map<String, Object> config = new HashMap<>();
        config.put("key", "value");
        assertTrue(provider.validateConfig(config));
    }
}
