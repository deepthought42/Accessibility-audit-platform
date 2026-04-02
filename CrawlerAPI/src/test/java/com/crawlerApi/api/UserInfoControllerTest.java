package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

@ExtendWith(MockitoExtension.class)
class UserInfoControllerTest {

    @Mock
    private Auth0Service auth0Service;

    @InjectMocks
    private UserInfoController controller;

    @Test
    void testGetCurrentUserAccountFound() {
        Principal principal = mock(Principal.class);
        Account account = new Account();
        account.setId(1L);
        when(auth0Service.getCurrentUserAccount(principal)).thenReturn(Optional.of(account));

        ResponseEntity<Account> response = controller.getCurrentUserAccount(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void testGetCurrentUserAccountNotFound() {
        Principal principal = mock(Principal.class);
        when(auth0Service.getCurrentUserAccount(principal)).thenReturn(Optional.empty());

        ResponseEntity<Account> response = controller.getCurrentUserAccount(principal);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetUserInfoSuccess() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", "John");
        when(auth0Service.getUserInfo("token123")).thenReturn(Optional.of(userInfo));

        ResponseEntity<Map<String, Object>> response = controller.getUserInfo("Bearer token123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("John", response.getBody().get("name"));
    }

    @Test
    void testGetUserInfoNotFound() {
        when(auth0Service.getUserInfo("token123")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getUserInfo("Bearer token123");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetUserInfoInvalidHeader() {
        ResponseEntity<Map<String, Object>> response = controller.getUserInfo("InvalidHeader");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetUserInfoNullHeader() {
        ResponseEntity<Map<String, Object>> response = controller.getUserInfo(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetUsernameSuccess() {
        when(auth0Service.getUsername("token123")).thenReturn(Optional.of("john"));

        ResponseEntity<String> response = controller.getUsername("Bearer token123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("john", response.getBody());
    }

    @Test
    void testGetUsernameNotFound() {
        when(auth0Service.getUsername("token123")).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getUsername("Bearer token123");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetUsernameInvalidHeader() {
        ResponseEntity<String> response = controller.getUsername("InvalidHeader");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetEmailSuccess() {
        when(auth0Service.getEmail("token123")).thenReturn(Optional.of("john@example.com"));

        ResponseEntity<String> response = controller.getEmail("Bearer token123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("john@example.com", response.getBody());
    }

    @Test
    void testGetEmailNotFound() {
        when(auth0Service.getEmail("token123")).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getEmail("Bearer token123");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetEmailInvalidHeader() {
        ResponseEntity<String> response = controller.getEmail("NoBearer");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetConfigStatusConfigured() {
        when(auth0Service.isConfigured()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.getConfigStatus();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("configured"));
        assertEquals("Auth0 is properly configured", response.getBody().get("message"));
    }

    @Test
    void testGetConfigStatusNotConfigured() {
        when(auth0Service.isConfigured()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.getConfigStatus();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("configured"));
        assertEquals("Auth0 is not configured", response.getBody().get("message"));
    }
}
