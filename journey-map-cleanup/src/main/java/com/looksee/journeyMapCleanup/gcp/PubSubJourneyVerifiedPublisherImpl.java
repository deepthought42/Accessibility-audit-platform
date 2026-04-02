package com.looksee.journeyMapCleanup.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubJourneyVerifiedPublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubJourneyVerifiedPublisherImpl.class);

    @Value("${pubsub.journey_verified}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}