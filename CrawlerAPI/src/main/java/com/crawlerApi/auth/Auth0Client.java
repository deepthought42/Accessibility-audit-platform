package com.crawlerApi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.crawlerApi.config.Auth0Config;

@Component
public class Auth0Client {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private AuthAPI auth0;
    private final Auth0Config auth0Config;

    @Autowired
    public Auth0Client(Auth0Config auth0Config) {
        this.auth0Config = auth0Config;
        initializeAuth0();
    }
    
    private void initializeAuth0() {
        if (auth0Config.getDomain() != null && 
            auth0Config.getClientId() != null && 
            auth0Config.getClientSecret() != null) {
            this.auth0 = new AuthAPI(
                auth0Config.getDomain(), 
                auth0Config.getClientId(), 
                auth0Config.getClientSecret()
            );
        } else {
            log.warn("Auth0 configuration is incomplete. Please check your auth0.properties file.");
        }
    }

    public AuthAPI getAuth0() {
        return auth0;
    }

    public String getUsername(String auth_access_token) {
        if (auth0 == null) {
            log.error("Auth0 API not initialized. Check configuration.");
            return null;
        }
        
        Request<UserInfo> user_info_request = auth0.userInfo(auth_access_token);
        String username = null;
        try {
            UserInfo info = user_info_request.execute();
            username = info.getValues().get("name").toString();
            log.info("Getting user Info ... " + username);
        } catch (APIException exception) {
            // api error
            log.error("Auth0 API error while retrieving username: {} - {}", exception.getError(), exception.getMessage(), exception);
        } catch (Auth0Exception exception) {
            // request error
            log.error("Auth0 request error while retrieving username", exception);
        }
        
        return username;
    }

    public String getNickname(String auth_access_token) {
        if (auth0 == null) {
            log.error("Auth0 API not initialized. Check configuration.");
            return null;
        }
        
        Request<UserInfo> user_info_request = auth0.userInfo(auth_access_token);
        String nickname = null;
        try {
            UserInfo info = user_info_request.execute();
            nickname = info.getValues().get("nickname").toString();
            log.info("Getting user Info ... " + nickname);
        } catch (APIException exception) {
            // api error
            log.error("Auth0 API error while retrieving nickname: {} - {}", exception.getError(), exception.getMessage(), exception);
        } catch (Auth0Exception exception) {
            // request error
            log.error("Auth0 request error while retrieving nickname", exception);
        }
        
        return nickname;
    }
}
