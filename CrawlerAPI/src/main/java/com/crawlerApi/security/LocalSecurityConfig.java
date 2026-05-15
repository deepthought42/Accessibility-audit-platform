package com.crawlerApi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Profile("local")
public class LocalSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Install an anonymous principal so HttpServletRequest.getUserPrincipal()
        // returns a non-null Principal whose getName() == "local-dev-user".
        // Several controllers (e.g. AuditorController) call
        // auth0Service.getCurrentUserAccount(principal) and then log
        // principal.getName(); a null principal NPEs there. The anonymous
        // authority lets Spring Security permit the request as part of the
        // anonymous-allowed chain rather than relying on permitAll alone.
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .anonymous(anon -> anon
                .principal("local-dev-user")
                .authorities("ROLE_USER"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Permissive CORS for local dev only - the class is @Profile("local")
        // so this bean never exists outside the compose stack. Wildcard origin
        // is safe here because credentials are explicitly disabled below.
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
