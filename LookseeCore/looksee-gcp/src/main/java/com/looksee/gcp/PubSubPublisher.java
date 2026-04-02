package com.looksee.gcp;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

import lombok.NoArgsConstructor;

/**
 * Abstract class for publishing to PubSub.
 */
@NoArgsConstructor
public abstract class PubSubPublisher {
    @Autowired
    private PubSubTemplate pubSubTemplate;

    /**
     * Returns the topic for the PubSub publisher.
     * @return the topic for the PubSub publisher
     *
     * precondition: topic() != null
     */
    protected abstract String topic();

    /**
     * Publishes a message to the PubSub topic.
     * @param audit_record_json the message to publish
     *
     * precondition: audit_record_json != null
     * precondition: !audit_record_json.isEmpty()
     * @throws ExecutionException if an error occurs
     * @throws InterruptedException if the thread is interrupted
     *
     * precondition: pubSubTemplate != null
     */
    public void publish(String audit_record_json) throws ExecutionException, InterruptedException {
        assert audit_record_json != null;
        assert !audit_record_json.isEmpty();
        pubSubTemplate.publish(topic(), audit_record_json).get();
    }
}
