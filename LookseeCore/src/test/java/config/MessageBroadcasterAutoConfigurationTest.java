package config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;

import com.looksee.config.MessageBroadcasterAutoConfiguration;
import com.looksee.services.MessageBroadcaster;
import com.pusher.rest.Pusher;

/**
 * Test class to verify that MessageBroadcasterAutoConfiguration always creates
 * MessageBroadcaster bean regardless of Pusher configuration.
 * 
 * This tests the lightweight auto-configuration that doesn't depend on Neo4j.
 * MessageBroadcasterAutoConfiguration imports PusherConfiguration to ensure
 * a Pusher bean (real or fallback) is always available.
 */
@ActiveProfiles("test")
class MessageBroadcasterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageBroadcasterAutoConfiguration.class))
            .withPropertyValues("spring.profiles.active=test");

    @Test
    void shouldCreateMessageBroadcasterWithoutPusherProperties() {
        contextRunner.run(context -> {
            // Verify beans are created
            assertThat(context).hasSingleBean(Pusher.class);
            assertThat(context).hasSingleBean(MessageBroadcaster.class);
            
            // Verify MessageBroadcaster is available
            MessageBroadcaster messageBroadcaster = context.getBean(MessageBroadcaster.class);
            assertThat(messageBroadcaster).isNotNull();
            
            // Should be using fallback mode
            assertThat(messageBroadcaster.isRealTimeMessagingEnabled()).isFalse();
        });
    }

    @Test
    void shouldCreateMessageBroadcasterWithPusherProperties() {
        contextRunner
            .withPropertyValues(
                "pusher.appId=test-app-id",
                "pusher.key=test-key",
                "pusher.secret=test-secret",
                "pusher.cluster=test-cluster"
            )
            .run(context -> {
                // Verify beans are created
                assertThat(context).hasSingleBean(Pusher.class);
                assertThat(context).hasSingleBean(MessageBroadcaster.class);
                
                // Verify MessageBroadcaster is available
                MessageBroadcaster messageBroadcaster = context.getBean(MessageBroadcaster.class);
                assertThat(messageBroadcaster).isNotNull();
                
                // Should be using real Pusher
                assertThat(messageBroadcaster.isRealTimeMessagingEnabled()).isTrue();
            });
    }

    @Test
    void shouldCreateMessageBroadcasterWithPartialPusherProperties() {
        contextRunner
            .withPropertyValues(
                "pusher.appId=test-app-id",
                "pusher.key=test-key"
                // Missing secret and cluster - should use fallback
            )
            .run(context -> {
                // Verify beans are created
                assertThat(context).hasSingleBean(Pusher.class);
                assertThat(context).hasSingleBean(MessageBroadcaster.class);
                
                // Verify MessageBroadcaster is available
                MessageBroadcaster messageBroadcaster = context.getBean(MessageBroadcaster.class);
                assertThat(messageBroadcaster).isNotNull();
                
                // Should be using fallback mode due to missing properties
                assertThat(messageBroadcaster.isRealTimeMessagingEnabled()).isFalse();
            });
    }
} 