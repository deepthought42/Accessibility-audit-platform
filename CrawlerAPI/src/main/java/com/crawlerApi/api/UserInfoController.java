package com.crawlerApi.api;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for retrieving user information from Auth0
 */
@Controller
@RequestMapping(path = "v1/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Info V1", description = "User Info API")
public class UserInfoController extends BaseApiController {
    
    @Autowired
    private Auth0Service auth0Service;
    
    /**
     * Get current user's account information
     * @param principal The authenticated principal
     * @return Account information
     */
    @GetMapping("/account")
    @Operation(summary = "Get current user account", description = "Get the current authenticated user's account information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account", content = @Content(schema = @Schema(type = "object", implementation = Account.class))),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Account> getCurrentUserAccount(Principal principal) {
        Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
        
        if (accountOpt.isPresent()) {
            return ResponseEntity.ok(accountOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user information from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return User information from Auth0
     */
    @GetMapping("/info")
    @Operation(summary = "Get user info from Auth0", description = "Get user information from Auth0 using the provided token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user info", content = @Content(schema = @Schema(type = "object"))),
        @ApiResponse(responseCode = "400", description = "Invalid authorization header"),
        @ApiResponse(responseCode = "404", description = "User info not found")
    })
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7); // Remove "Bearer " prefix
        Optional<Map<String, Object>> userInfoOpt = auth0Service.getUserInfo(accessToken);
        
        if (userInfoOpt.isPresent()) {
            return ResponseEntity.ok(userInfoOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get username from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return Username
     */
    @GetMapping("/username")
    @Operation(summary = "Get username from Auth0", description = "Get username from Auth0 using the provided token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved username", content = @Content(schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid authorization header"),
        @ApiResponse(responseCode = "404", description = "Username not found")
    })
    public ResponseEntity<String> getUsername(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7);
        Optional<String> usernameOpt = auth0Service.getUsername(accessToken);
        
        if (usernameOpt.isPresent()) {
            return ResponseEntity.ok(usernameOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user email from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return Email address
     */
    @GetMapping("/email")
    @Operation(summary = "Get user email from Auth0", description = "Get user email from Auth0 using the provided token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved email", content = @Content(schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid authorization header"),
        @ApiResponse(responseCode = "404", description = "Email not found")
    })
    public ResponseEntity<String> getEmail(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7);
        Optional<String> emailOpt = auth0Service.getEmail(accessToken);
        
        if (emailOpt.isPresent()) {
            return ResponseEntity.ok(emailOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Check if Auth0 is properly configured
     * @return Configuration status
     */
    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        boolean isConfigured = auth0Service.isConfigured();
        
        Map<String, Object> status = Map.of(
            "configured", isConfigured,
            "message", isConfigured ? "Auth0 is properly configured" : "Auth0 is not configured"
        );
        
        return ResponseEntity.ok(status);
    }
} 