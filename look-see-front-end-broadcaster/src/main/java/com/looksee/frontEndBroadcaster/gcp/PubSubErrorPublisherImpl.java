package com.looksee.frontEndBroadcaster.gcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubErrorPublisherImpl extends PubSubPublisher {
    @Value("${pubsub.error_topic}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}