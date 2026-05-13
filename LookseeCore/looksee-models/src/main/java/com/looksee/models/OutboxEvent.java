package com.looksee.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Node
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue
    private Long id;

    private String eventId;
    private String topic;
    private String payload;
    private String status;
    private String correlationId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime nextAttemptAt;
    private int retryCount;

    public OutboxEvent(String topic, String payload) {
        this(topic, payload, null);
    }

    public OutboxEvent(String topic, String payload, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING.name();
        this.correlationId = correlationId;
        this.createdAt = LocalDateTime.now();
        this.nextAttemptAt = this.createdAt;
        this.retryCount = 0;
    }
}
