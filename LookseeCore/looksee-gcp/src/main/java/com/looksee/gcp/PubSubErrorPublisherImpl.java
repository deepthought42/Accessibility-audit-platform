package com.looksee.gcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

/**
 * Implementation of {@link PubSubPublisher} for errors
 */
@Component
@ConditionalOnClass(name = "com.google.cloud.spring.pubsub.core.PubSubTemplate")
@ConditionalOnBean(PubSubTemplate.class)
@ConditionalOnProperty(name = "pubsub.error_topic")
public class PubSubErrorPublisherImpl extends PubSubPublisher {
    @Value("${pubsub.error_topic}")
    private String topic;
    
    /**
     * Returns the topic for errors
     * @return the topic for errors
     */
    @Override
    protected String topic() {
        return this.topic;
    }
}