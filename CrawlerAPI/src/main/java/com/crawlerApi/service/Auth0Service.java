package com.crawlerApi.service;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.crawlerApi.config.Auth0Config;
import com.looksee.models.Account;
import com.looksee.services.AccountService;

@Service
public class Auth0Service {
    
    private static final Logger log = LoggerFactory.getLogger(Auth0Service.class);
    
    private final Auth0Config auth0Config;
    private final AccountService accountService;
    private AuthAPI auth0;
    
    @Autowired
    public Auth0Service(Auth0Config auth0Config, AccountService accountService) {
        this.auth0Config = auth0Config;
        this.accountService = accountService;
        initializeAuth0();
    }
    
    private void initializeAuth0() {
        if (auth0Config.getAuth0Domain() != null && 
            auth0Config.getAuth0ClientId() != null && 
            auth0Config.getAuth0ClientSecret() != null) {
            this.auth0 = new AuthAPI(
                auth0Config.getAuth0Domain(), 
                auth0Config.getAuth0ClientId(), 
                auth0Config.getAuth0ClientSecret()
            );
            log.info("Auth0 API initialized successfully");
        } else {
            log.warn("Auth0 configuration is incomplete. Please check your auth0.properties file.");
        }
    }
    
    /**
     * Get the current user's account based on the principal
     * @param principal The authenticated principal
     * @return Optional containing the Account if found
     */
    public Optional<Account> getCurrentUserAccount(Principal principal) {
        if (principal == null) {
            log.warn("Principal is null");
            return Optional.empty();
        }
        
        String userId = extractUserId(principal.getName());
        return Optional.ofNullable(accountService.findByUserId(userId));
    }
    
    /**
     * Get user information from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing user information map
     */
    public Optional<Map<String, Object>> getUserInfo(String accessToken) {
        if (auth0 == null) {
            log.error("Auth0 API not initialized. Check configuration.");
            return Optional.empty();
        }
        
        try {
            Request<UserInfo> userInfoRequest = auth0.userInfo(accessToken);
            UserInfo userInfo = userInfoRequest.execute();
            return Optional.of(userInfo.getValues());
        } catch (APIException exception) {
            log.error("API error getting user info: " + exception.getError() + " - " + exception.getMessage());
        } catch (Auth0Exception exception) {
            log.error("Auth0 error getting user info: " + exception.getMessage());
        } catch (Exception exception) {
            log.error("Unexpected error getting user info: " + exception.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get username from Auth0 user info
     * @param accessToken The Auth0 access token
     * @return Optional containing the username
     */
    public Optional<String> getUsername(String accessToken) {
        return getUserInfo(accessToken)
            .map(userInfo -> userInfo.get("name"))
            .map(Object::toString);
    }
    
    /**
     * Get nickname from Auth0 user info
     * @param accessToken The Auth0 access token
     * @return Optional containing the nickname
     */
    public Optional<String> getNickname(String accessToken) {
        return getUserInfo(accessToken)
            .map(userInfo -> userInfo.get("nickname"))
            .map(Object::toString);
    }
    
    /**
     * Get email from Auth0 user info
     * @param accessToken The Auth0 access token
     * @return Optional containing the email
     */
    public Optional<String> getEmail(String accessToken) {
        return getUserInfo(accessToken)
            .map(userInfo -> userInfo.get("email"))
            .map(Object::toString);
    }
    
    /**
     * Extract user ID from Auth0 principal name
     * @param principalName The principal name (usually contains "auth0|" prefix)
     * @return The user ID without the "auth0|" prefix
     */
    public String extractUserId(String principalName) {
        if (principalName == null) {
            return null;
        }
        return principalName.replace("auth0|", "");
    }
    
    /**
     * Get Auth0 API instance
     * @return The AuthAPI instance
     */
    public AuthAPI getAuth0() {
        return auth0;
    }
    
    /**
     * Check if Auth0 is properly configured
     * @return true if Auth0 is configured and initialized
     */
    public boolean isConfigured() {
        return auth0 != null && 
               auth0Config.getAuth0Domain() != null && 
               auth0Config.getAuth0ClientId() != null && 
               auth0Config.getAuth0ClientSecret() != null;
    }
} 