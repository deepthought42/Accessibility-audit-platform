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
 * Implementation of {@link PubSubPublisher} for journey candidates
 */
@Component
@ConditionalOnClass(name = "com.google.cloud.spring.pubsub.core.PubSubTemplate")
@ConditionalOnBean(PubSubTemplate.class)
@ConditionalOnProperty(name = "pubsub.journey_candidate")
public class PubSubJourneyCandidatePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubJourneyCandidatePublisherImpl.class);

    @Value("${pubsub.journey_candidate}")
    private String topic;
    
    /**
     * Returns the topic for journey candidates
     * @return the topic for journey candidates
     */
    @Override
    protected String topic() {
        return this.topic;
    }
}