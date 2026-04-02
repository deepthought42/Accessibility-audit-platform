package com.looksee.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

/**
 * Implementation of {@link PubSubPublisher} for URL messages
 */
@Component
@ConditionalOnClass(name = "com.google.cloud.spring.pubsub.core.PubSubTemplate")
@ConditionalOnBean(PubSubTemplate.class)
@ConditionalOnProperty(name = "pubsub.url_topic")
public class PubSubUrlMessagePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubUrlMessagePublisherImpl.class);

    @Value("${pubsub.url_topic}")
    private String topic;
    
    /**
     * Returns the topic for URL messages
     * @return the topic for URL messages
     */
    @Override
    protected String topic() {
        return this.topic;
    }
}