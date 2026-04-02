package com.crawlerApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Configuration
@ConfigurationProperties(prefix = "auth0")
@Getter
@Setter
public class Auth0Config {
    
    private String domain;
    private String issuer;
    private String apiAudience;
    private String clientId;
    private String clientSecret;
    private String audience;
    private String securedRoute;
    private boolean base64EncodedSecret;
    private String authorityStrategy;
    private boolean defaultAuth0ApiSecurityEnabled;
    private String signingAlgorithm;
    
    // Additional methods for user information retrieval
    public String getAuth0Domain() {
        return domain;
    }
    
    public String getAuth0Issuer() {
        return issuer;
    }
    
    public String getAuth0ClientId() {
        return clientId;
    }
    
    public String getAuth0ClientSecret() {
        return clientSecret;
    }
    
    public String getAuth0Audience() {
        return audience;
    }
    
    public String getAuth0ApiAudience() {
        return apiAudience;
    }
} 