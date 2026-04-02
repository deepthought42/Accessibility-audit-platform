package com.looksee.gcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

/**
 * Implementation of {@link PubSubPublisher} for page building
 */
@Component
@ConditionalOnClass(name = "com.google.cloud.spring.pubsub.core.PubSubTemplate")
@ConditionalOnBean(PubSubTemplate.class)
@ConditionalOnProperty(name = "pubsub.page_built")
public class PubSubPageBuiltPublisherImpl extends PubSubPublisher {

    @Value("${pubsub.page_built}")
    private String topic;
    
    /**
     * Returns the topic for page building
     * @return the topic for page building
     */
    @Override
    protected String topic() {
        return this.topic;
    }
}