package com.looksee.pageBuilder.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubErrorPublisherImpl extends PubSubPublisher {

    private static Logger LOG = LoggerFactory.getLogger(PubSubErrorPublisherImpl.class);

    @Value("${pubsub.error_topic}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}