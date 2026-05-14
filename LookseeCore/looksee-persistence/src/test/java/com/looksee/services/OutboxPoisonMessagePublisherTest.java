package com.looksee.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import com.looksee.models.message.PoisonMessageEnvelope;
import com.looksee.models.repository.OutboxEventRepository;

/**
 * The adapter wraps {@link OutboxPublishingGateway#enqueueOutOfBand} for
 * the poison topic. The gateway silently no-ops when its repository is
 * not wired, so the adapter must verify the repository is present and
 * fail closed when it is missing — otherwise {@code publishPoison}
 * would look successful to {@code PubSubAuditController} while no
 * poison row was actually staged.
 */
class OutboxPoisonMessagePublisherTest {

    private static final String TOPIC = "looksee.poison";

    private static ApplicationContext schedulingEnabledContext() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME))
            .thenReturn(true);
        return ctx;
    }

    @Test
    void publishPoison_delegatesToGateway_whenRepositoryWired() {
        OutboxPublishingGateway gateway = mock(OutboxPublishingGateway.class);
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        OutboxPoisonMessagePublisher publisher = new OutboxPoisonMessagePublisher(
            gateway, repo, TOPIC, schedulingEnabledContext());

        PoisonMessageEnvelope envelope = new PoisonMessageEnvelope();
        publisher.publishPoison(envelope, "trace-1");

        verify(gateway, times(1)).enqueueOutOfBand(TOPIC, envelope, "trace-1");
    }

    @Test
    void publishPoison_failsClosed_whenRepositoryNotWired() {
        OutboxPublishingGateway gateway = mock(OutboxPublishingGateway.class);
        OutboxPoisonMessagePublisher publisher = new OutboxPoisonMessagePublisher(
            gateway, null, TOPIC, schedulingEnabledContext());

        // The gateway's save() silently no-ops in this state. The adapter
        // must surface the misconfiguration so the controller returns 500
        // and Pub/Sub redelivers — not 200 with no poison row staged.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> publisher.publishPoison(new PoisonMessageEnvelope(), "trace-2"));
        assertTrue(ex.getMessage().contains("OutboxEventRepository"),
            "expected the message to name the missing dependency, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(TOPIC),
            "expected the message to include the topic, got: " + ex.getMessage());
        verify(gateway, never()).enqueueOutOfBand(any(), any(), any());
    }

    @Test
    void publishPoison_propagatesGatewayFailure() {
        OutboxPublishingGateway gateway = mock(OutboxPublishingGateway.class);
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        RuntimeException gatewayFailure = new RuntimeException("outbox down");
        doThrow(gatewayFailure).when(gateway).enqueueOutOfBand(any(), any(), any());
        OutboxPoisonMessagePublisher publisher = new OutboxPoisonMessagePublisher(
            gateway, repo, TOPIC, schedulingEnabledContext());

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> publisher.publishPoison(new PoisonMessageEnvelope(), "trace-3"));
        assertEquals(gatewayFailure, thrown);
    }

    @Test
    void verifySchedulingEnabled_throws_whenSchedulingDisabled() {
        // The adapter stages outbox rows that only get drained when
        // @EnableScheduling is enabled somewhere in the context. If a
        // service opts in via pubsub.poison but forgets scheduling,
        // poison rows would pile up unsent — fail startup loudly.
        OutboxPublishingGateway gateway = mock(OutboxPublishingGateway.class);
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME))
            .thenReturn(false);
        OutboxPoisonMessagePublisher publisher = new OutboxPoisonMessagePublisher(
            gateway, repo, TOPIC, ctx);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            publisher::verifySchedulingEnabled);
        assertTrue(ex.getMessage().contains("@EnableScheduling"),
            "expected the message to name @EnableScheduling, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(TOPIC),
            "expected the message to include the topic, got: " + ex.getMessage());
    }

    @Test
    void verifySchedulingEnabled_passes_whenSchedulingEnabled() {
        OutboxPublishingGateway gateway = mock(OutboxPublishingGateway.class);
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        OutboxPoisonMessagePublisher publisher = new OutboxPoisonMessagePublisher(
            gateway, repo, TOPIC, schedulingEnabledContext());

        // No exception — startup succeeds.
        publisher.verifySchedulingEnabled();
    }
}
