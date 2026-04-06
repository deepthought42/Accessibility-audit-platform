package com.looksee.gcp;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class PubSubPublisher {
    private static final Logger log = LoggerFactory.getLogger(PubSubPublisher.class);
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    @Autowired
    private PubSubTemplate pubSubTemplate;

    protected abstract String topic();

    public void publish(String message) throws ExecutionException, InterruptedException {
        assert message != null;
        assert !message.isEmpty();

        ExecutionException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                pubSubTemplate.publish(topic(), message).get();
                log.debug("Published message to topic={} size={} attempt={}", topic(), message.length(), attempt);
                return;
            } catch (ExecutionException e) {
                lastException = e;
                log.warn("Publish failed for topic={} attempt={}/{}: {}", topic(), attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
                }
            }
        }
        log.error("All {} publish attempts failed for topic={}", MAX_RETRIES, topic());
        throw lastException;
    }
}
