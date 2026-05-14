package com.looksee.models.message;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wrapper for a Pub/Sub message that arrived but could not be processed
 * (bad Base64, malformed JSON, empty envelope, etc.). Published to the
 * shared {@code looksee.poison} topic so operators have a single
 * subscription capturing every message the platform failed to understand,
 * separate from the per-topic dead-letter queues that only capture
 * transient-failure exhaustion.
 *
 * <p>Carries the original Pub/Sub message id and the verbatim Base64
 * payload so an operator can replay or inspect the bad message. Trace
 * attributes are preserved so the poison row joins the original
 * distributed trace.
 */
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoisonMessageEnvelope {

    private String serviceName;
    private String topic;
    private String originalMessageId;
    private Map<String, String> attributes = new HashMap<>();
    private String base64Data;
    private String errorClass;
    private String errorMessage;
    private String timestamp;

    public PoisonMessageEnvelope(
        String serviceName,
        String topic,
        String originalMessageId,
        Map<String, String> attributes,
        String base64Data,
        String errorClass,
        String errorMessage
    ) {
        this.serviceName = serviceName;
        this.topic = topic;
        this.originalMessageId = originalMessageId;
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.base64Data = base64Data;
        this.errorClass = errorClass;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now().toString();
    }
}
