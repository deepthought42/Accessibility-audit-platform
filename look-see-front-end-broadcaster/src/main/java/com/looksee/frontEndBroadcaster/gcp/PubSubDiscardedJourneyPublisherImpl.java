package com.looksee.frontEndBroadcaster.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubDiscardedJourneyPublisherImpl extends PubSubPublisher {
    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubDiscardedJourneyPublisherImpl.class);

    @Value("${pubsub.discarded_journey_topic}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}