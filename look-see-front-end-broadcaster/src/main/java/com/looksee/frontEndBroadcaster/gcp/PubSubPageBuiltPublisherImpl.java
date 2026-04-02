package com.looksee.frontEndBroadcaster.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubPageBuiltPublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubPageBuiltPublisherImpl.class);

    @Value("${pubsub.page_built}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}