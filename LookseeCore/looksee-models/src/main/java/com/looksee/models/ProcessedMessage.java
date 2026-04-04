package com.looksee.models;

import java.time.LocalDateTime;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks PubSub messages that have already been processed, providing
 * exactly-once processing semantics via message deduplication.
 *
 * <p>Each record stores the PubSub-assigned {@code pubsubMessageId} (unique
 * per delivery), the {@code serviceName} that processed it, and metadata
 * about the processing outcome.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>{@code pubsubMessageId} is never null or empty</li>
 *   <li>{@code serviceName} is never null or empty</li>
 *   <li>The combination of (pubsubMessageId, serviceName) is unique</li>
 * </ul>
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class ProcessedMessage {
    @Id
    @GeneratedValue
    private Long id;

    private String pubsubMessageId;
    private String serviceName;
    private String status;
    private LocalDateTime processedAt;

    /**
     * Creates a new ProcessedMessage record.
     *
     * @param pubsubMessageId the PubSub envelope message ID
     * @param serviceName the name of the service processing this message
     */
    public ProcessedMessage(String pubsubMessageId, String serviceName) {
        assert pubsubMessageId != null && !pubsubMessageId.isEmpty() : "pubsubMessageId must not be null or empty";
        assert serviceName != null && !serviceName.isEmpty() : "serviceName must not be null or empty";

        this.pubsubMessageId = pubsubMessageId;
        this.serviceName = serviceName;
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
    }
}
