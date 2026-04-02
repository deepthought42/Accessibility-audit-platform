package com.looksee.pageBuilder.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubPageCreatedPublisherImpl extends PubSubPublisher {

    private static Logger LOG = LoggerFactory.getLogger(PubSubPageCreatedPublisherImpl.class);

    @Value("${pubsub.page_built}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}