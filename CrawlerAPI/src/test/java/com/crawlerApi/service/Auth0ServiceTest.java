package com.crawlerApi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crawlerApi.config.Auth0Config;
import com.looksee.models.Account;
import com.looksee.services.AccountService;

@ExtendWith(MockitoExtension.class)
class Auth0ServiceTest {

    @Mock
    private Auth0Config auth0Config;

    @Mock
    private AccountService accountService;

    private Auth0Service auth0Service;

    @BeforeEach
    void setUp() {
        // Auth0Config returns nulls by default (mock), so auth0 won't be initialized
        auth0Service = new Auth0Service(auth0Config, accountService);
    }

    @Test
    void testExtractUserIdWithAuth0Prefix() {
        String result = auth0Service.extractUserId("auth0|12345");
        assertEquals("12345", result);
    }

    @Test
    void testExtractUserIdWithoutPrefix() {
        String result = auth0Service.extractUserId("12345");
        assertEquals("12345", result);
    }

    @Test
    void testExtractUserIdNull() {
        assertNull(auth0Service.extractUserId(null));
    }

    @Test
    void testGetCurrentUserAccountNullPrincipal() {
        Optional<Account> result = auth0Service.getCurrentUserAccount(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCurrentUserAccountFoundAccount() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("auth0|user123");
        Account account = new Account();
        account.setId(1L);
        when(accountService.findByUserId("user123")).thenReturn(account);

        Optional<Account> result = auth0Service.getCurrentUserAccount(principal);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testGetCurrentUserAccountNotFound() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("auth0|unknown");
        when(accountService.findByUserId("unknown")).thenReturn(null);

        Optional<Account> result = auth0Service.getCurrentUserAccount(principal);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserInfoWhenNotInitialized() {
        // auth0 is null since config values are null
        Optional<?> result = auth0Service.getUserInfo("some-token");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUsernameWhenNotInitialized() {
        Optional<String> result = auth0Service.getUsername("some-token");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNicknameWhenNotInitialized() {
        Optional<String> result = auth0Service.getNickname("some-token");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmailWhenNotInitialized() {
        Optional<String> result = auth0Service.getEmail("some-token");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAuth0WhenNotInitialized() {
        assertNull(auth0Service.getAuth0());
    }

    @Test
    void testIsConfiguredWhenNotInitialized() {
        assertFalse(auth0Service.isConfigured());
    }

    @Test
    void testIsConfiguredWhenPartiallyConfigured() {
        when(auth0Config.getAuth0Domain()).thenReturn("domain.auth0.com");
        // clientId and clientSecret still null
        Auth0Service partialService = new Auth0Service(auth0Config, accountService);
        assertFalse(partialService.isConfigured());
    }

    @Test
    void testExtractUserIdEmptyString() {
        String result = auth0Service.extractUserId("");
        assertEquals("", result);
    }

    @Test
    void testExtractUserIdMultiplePipes() {
        String result = auth0Service.extractUserId("auth0|prefix|12345");
        assertEquals("prefix|12345", result);
    }
}
