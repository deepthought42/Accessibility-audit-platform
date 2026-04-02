package com.crawlerApi.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.crawlerApi.config.Auth0Config;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@Component
public class Auth0ManagementApi {
    private static final Logger log = LoggerFactory.getLogger(Auth0ManagementApi.class);
    
    private final Auth0Config auth0Config;
    private String baseUrl;
    private String audienceUrl;
    private String apiToken;

    @Autowired
    public Auth0ManagementApi(Auth0Config auth0Config) {
        this.auth0Config = auth0Config;
        initialize();
    }
    
    private void initialize() {
        if (auth0Config.getDomain() != null) {
            this.baseUrl = "https://" + auth0Config.getDomain() + "/";
            this.audienceUrl = baseUrl + "api/v2/";
            // You'll need to set the API token separately or get it from Auth0
            this.apiToken = "your-api-token";
        } else {
            log.warn("Auth0 domain not configured. Please check your auth0.properties file.");
        }
    }

    public String getToken() throws UnirestException {
        if (baseUrl == null) {
            log.error("Auth0 Management API not initialized. Check configuration.");
            return null;
        }
        
        if (auth0Config.getClientId() == null || auth0Config.getClientSecret() == null) {
            log.error("Auth0 client credentials not configured. Please check your auth0.properties file.");
            return null;
        }
        
        HttpResponse<String> response1 = Unirest.post(baseUrl + "oauth/token")
                .header("content-type", "application/json")
                .body("{\"client_id\":\"" + auth0Config.getClientId() +
                    "\",\"client_secret\":\"" + auth0Config.getClientSecret() +
                    "\",\"audience\":\"" + audienceUrl +
                    "\",\"grant_type\":\"client_credentials\"}")
                .asString();
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> jsonMap = mapper.readValue(response1.getBody(), new TypeReference<Map<String, Object>>() {});
            return (String) jsonMap.get("access_token");
        } catch (JsonGenerationException e) {
            log.error("Error generating JSON", e);
        } catch (JsonMappingException e) {
            log.error("Error mapping JSON", e);
        } catch (Exception e) {
            log.error("Error parsing response", e);
        }
        return null;
    }

    public HttpResponse<String> deleteUser(String user_id) throws UnirestException {
        if (baseUrl == null) {
            log.error("Auth0 Management API not initialized. Check configuration.");
            return null;
        }
        
        String token = getToken();
        if (token == null) {
            log.error("Failed to get Auth0 management token.");
            return null;
        }
        
        HttpResponse<String> response = Unirest.delete(audienceUrl + "users/" + user_id)
                .header("authorization", "Bearer " + token)
                .asString();
        return response;
    }
} 