package com.crawlerApi.security;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.crawlerApi.config.Auth0Config;
import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

@EnableWebSecurity
@PropertySource("classpath:auth0.properties")
@Component
public class SecurityConfig {
	
	private final Auth0Config auth0Config;
	private final Auth0Service auth0Service;
	
	@Autowired
	public SecurityConfig(Auth0Config auth0Config, Auth0Service auth0Service) {
		this.auth0Config = auth0Config;
		this.auth0Service = auth0Service;
	}
	
    @Bean
    @Profile("!local")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/actuator/info")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auditor/start-individual")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/audits")).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .cors();
        return http.build();
    }

    @Bean
    @Profile("!local")
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(auth0Config.getAuth0Issuer());
        
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(auth0Config.getAuth0Issuer());
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            String expectedAudience = auth0Config.getApiAudience();
            if (expectedAudience == null || expectedAudience.isBlank()) {
                expectedAudience = auth0Config.getAudience();
            }
            if (expectedAudience == null || expectedAudience.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }

            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        };

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withTimestamp, audienceValidator);

        jwtDecoder.setJwtValidator(validator);
        
        return jwtDecoder;
    }
    
    /**
     * Get the current user's account
     * @param principal The authenticated principal
     * @return Optional containing the Account if found
     */
    public Optional<Account> getCurrentUserAccount(Principal principal) {
        return auth0Service.getCurrentUserAccount(principal);
    }
    
    /**
     * Get user information from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing user information
     */
    public Optional<String> getUsername(String accessToken) {
        return auth0Service.getUsername(accessToken);
    }
    
    /**
     * Get user nickname from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing the nickname
     */
    public Optional<String> getNickname(String accessToken) {
        return auth0Service.getNickname(accessToken);
    }
    
    /**
     * Get user email from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing the email
     */
    public Optional<String> getEmail(String accessToken) {
        return auth0Service.getEmail(accessToken);
    }
    
    /**
     * Extract user ID from Auth0 principal name
     * @param principalName The principal name
     * @return The user ID without the "auth0|" prefix
     */
    public String extractUserId(String principalName) {
        return auth0Service.extractUserId(principalName);
    }
    
    /**
     * Check if Auth0 is properly configured
     * @return true if Auth0 is configured and initialized
     */
    public boolean isAuth0Configured() {
        return auth0Service.isConfigured();
    }
	
    @Bean
    @Profile("!local")
	CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}