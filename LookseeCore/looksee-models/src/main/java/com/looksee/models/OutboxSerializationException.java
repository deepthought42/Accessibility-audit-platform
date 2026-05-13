package com.looksee.models;

/**
 * Thrown when an outbox payload cannot be serialized to JSON for storage.
 *
 * <p>Wraps the Jackson {@code JsonProcessingException} so the
 * {@code OutboxPublishingGateway} can throw an unchecked exception from
 * {@code enqueue}/{@code enqueueOutOfBand} without polluting every call
 * site with a {@code throws} clause.</p>
 */
public class OutboxSerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OutboxSerializationException(Object payload, Throwable cause) {
        super("failed to serialize outbox payload of type "
                + (payload == null ? "null" : payload.getClass().getName()), cause);
    }
}
