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
 * Implementation of {@link PubSubPublisher} for audit updates
 */
@Component
@ConditionalOnClass(name = "com.google.cloud.spring.pubsub.core.PubSubTemplate")
@ConditionalOnBean(PubSubTemplate.class)
@ConditionalOnProperty(name = "pubsub.audit_update")
public class PubSubAuditUpdatePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubAuditUpdatePublisherImpl.class);

    @Value("${pubsub.audit_update}")
    private String topic;
    
    /**
     * Returns the topic for audit updates
     * @return the topic for audit updates
     */
    @Override
    protected String topic() {
        return this.topic;
    }
}