package com.looksee.frontEndBroadcaster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pusher.rest.Pusher;

import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines methods for emitting data to subscribed clients
 */
@NoArgsConstructor
@Component
public class PusherConnector {
    @Setter
    private Pusher pusher;

    @Value("${pusher.appId}") 
    String appId;
    @Value("${pusher.key}") 
    String key;
    @Value("${pusher.secret}") 
    String secret;
    @Value("${pusher.cluster}") 
    String cluster;

    public PusherConnector( @Value("${pusher.appId}") String appId,
                            @Value("${pusher.key}") String key,
                            @Value("${pusher.secret}") String secret,
                            @Value("${pusher.cluster}") String cluster)
    {
        Pusher pusher = new Pusher(appId, key, secret);
		pusher.setCluster(cluster);
		pusher.setEncrypted(true);
        setPusher(pusher);
	}
	
    public Pusher getPusher(){
        Pusher pusher = new Pusher(appId, key, secret);
		pusher.setCluster(cluster);
		pusher.setEncrypted(true);
        return pusher;
    }

}
