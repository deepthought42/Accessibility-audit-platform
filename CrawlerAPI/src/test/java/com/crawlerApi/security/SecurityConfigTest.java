package com.crawlerApi.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.CorsConfigurationSource;

import com.crawlerApi.config.Auth0Config;
import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private Auth0Config auth0Config;

    @Mock
    private Auth0Service auth0Service;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(auth0Config, auth0Service);
    }

    @Test
    void testGetCurrentUserAccount() {
        Principal principal = mock(Principal.class);
        Account account = new Account();
        account.setId(1L);
        when(auth0Service.getCurrentUserAccount(principal)).thenReturn(Optional.of(account));

        Optional<Account> result = securityConfig.getCurrentUserAccount(principal);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testGetUsername() {
        when(auth0Service.getUsername("token")).thenReturn(Optional.of("john"));
        Optional<String> result = securityConfig.getUsername("token");
        assertTrue(result.isPresent());
        assertEquals("john", result.get());
    }

    @Test
    void testGetNickname() {
        when(auth0Service.getNickname("token")).thenReturn(Optional.of("johnny"));
        Optional<String> result = securityConfig.getNickname("token");
        assertTrue(result.isPresent());
        assertEquals("johnny", result.get());
    }

    @Test
    void testGetEmail() {
        when(auth0Service.getEmail("token")).thenReturn(Optional.of("john@example.com"));
        Optional<String> result = securityConfig.getEmail("token");
        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get());
    }

    @Test
    void testExtractUserId() {
        when(auth0Service.extractUserId("auth0|12345")).thenReturn("12345");
        String result = securityConfig.extractUserId("auth0|12345");
        assertEquals("12345", result);
    }

    @Test
    void testIsAuth0Configured() {
        when(auth0Service.isConfigured()).thenReturn(true);
        assertTrue(securityConfig.isAuth0Configured());
    }

    @Test
    void testIsAuth0ConfiguredFalse() {
        when(auth0Service.isConfigured()).thenReturn(false);
        assertFalse(securityConfig.isAuth0Configured());
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
    }

    @Test
    void testGetUsernameEmpty() {
        when(auth0Service.getUsername("token")).thenReturn(Optional.empty());
        Optional<String> result = securityConfig.getUsername("token");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmailEmpty() {
        when(auth0Service.getEmail("token")).thenReturn(Optional.empty());
        Optional<String> result = securityConfig.getEmail("token");
        assertTrue(result.isEmpty());
    }
}
