package com.looksee.pageBuilder.gcp;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

public abstract class PubSubPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(PubSubPublisher.class);

    @Autowired
    private PubSubTemplate pubSubTemplate;

    protected abstract String topic();

    public void publish(String audit_record_json) throws ExecutionException, InterruptedException {
        LOG.info("Publishing to the topic [{}], message [{}]", topic(), audit_record_json);
        pubSubTemplate.publish(topic(), audit_record_json).get();
    }
}
