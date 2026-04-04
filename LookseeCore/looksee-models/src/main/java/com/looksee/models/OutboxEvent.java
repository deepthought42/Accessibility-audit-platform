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
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private int retryCount;

    public OutboxEvent(String topic, String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
}
