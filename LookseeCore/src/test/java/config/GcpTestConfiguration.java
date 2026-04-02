package config;

import static org.mockito.Mockito.*;

import org.neo4j.driver.Driver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jTemplate;

import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.ImageAnnotatorClient;

/**
 * Test configuration class to provide mock beans for Google Cloud Platform services
 * and Neo4j services that may not be available during testing.
 * This prevents authentication and dependency errors in test environments.
 */
@TestConfiguration
public class GcpTestConfiguration {

    /**
     * Provides a mock Storage bean for testing to avoid GCP dependencies
     * This prevents the "Your default credentials were not found" error
     */
    @Bean
    @Primary
    public Storage mockStorage() {
        return mock(Storage.class);
    }

    /**
     * Provides a mock PubSubTemplate bean for testing to avoid GCP dependencies
     * This prevents the "The project ID can't be null or empty" error
     */
    @Bean
    @Primary
    public Object mockPubSubTemplate() {
        // Use Object type to avoid importing PubSubTemplate if not available
        return mock(Object.class);
    }

    /**
     * Provides a mock ImageAnnotatorClient bean for testing to avoid GCP Vision dependencies
     * This prevents the "Your default credentials were not found" error for Vision API
     */
    @Bean
    @Primary
    public ImageAnnotatorClient mockImageAnnotatorClient() {
        return mock(ImageAnnotatorClient.class);
    }

    /**
     * Provides a mock DocumentOcrTemplate bean for testing to avoid GCP Vision dependencies
     * This prevents Vision OCR template creation issues in tests
     */
    @Bean
    @Primary
    public Object mockDocumentOcrTemplate() {
        // Use Object type to avoid importing DocumentOcrTemplate if not available
        return mock(Object.class);
    }

    /**
     * Provides a mock Neo4j Driver bean for testing to avoid Neo4j database dependencies
     * This prevents the "Cannot connect to Neo4j database" error in tests
     */
    @Bean
    @Primary
    public Driver mockNeo4jDriver() {
        return mock(Driver.class);
    }

    /**
     * Provides a mock Neo4j Template bean for testing to avoid Neo4j database dependencies
     * This prevents the "No bean named 'neo4jTemplate' available" error in tests
     */
    @Bean
    @Primary
    public Neo4jTemplate mockNeo4jTemplate() {
        return mock(Neo4jTemplate.class);
    }
} 