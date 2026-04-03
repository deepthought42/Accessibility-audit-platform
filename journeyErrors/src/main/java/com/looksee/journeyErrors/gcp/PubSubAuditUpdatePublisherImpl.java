package com.looksee.journeyErrors.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.looksee.gcp.PubSubPublisher;

@Component
public class PubSubAuditUpdatePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubAuditUpdatePublisherImpl.class);

    @Value("${pubsub.audit_update}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}