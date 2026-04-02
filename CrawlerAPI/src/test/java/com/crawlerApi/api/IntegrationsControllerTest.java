package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.crawlerApi.integrations.IntegrationFacade;
import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationProvider;
import com.crawlerApi.integrations.IntegrationWithConfigResponse;
import com.looksee.models.Account;
import com.looksee.services.AccountService;

@ExtendWith(MockitoExtension.class)
class IntegrationsControllerTest {

    @Mock
    private IntegrationFacade integrationFacade;

    @Mock
    private AccountService accountService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private IntegrationsController controller;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);

        Principal principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("auth0|user1");
        lenient().when(request.getUserPrincipal()).thenReturn(principal);
        lenient().when(accountService.findByUserId("user1")).thenReturn(testAccount);
    }

    @Test
    void testListIntegrations() throws Exception {
        List<IntegrationMetadata> metadata = Arrays.asList(
            new IntegrationMetadata("jira", "Jira", "PM", Collections.emptyList())
        );
        when(integrationFacade.listAvailableIntegrations()).thenReturn(metadata);

        ResponseEntity<List<IntegrationMetadata>> response = controller.list(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetByTypeFound() throws Exception {
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "PM", Collections.emptyList());
        when(integrationFacade.getMetadata("jira")).thenReturn(Optional.of(metadata));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "***");
        when(integrationFacade.getConfigMasked(1L, "jira")).thenReturn(Optional.of(config));

        ResponseEntity<IntegrationWithConfigResponse> response = controller.getByType(request, "jira");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jira", response.getBody().getMetadata().getId());
    }

    @Test
    void testGetByTypeNotFound() throws Exception {
        when(integrationFacade.getMetadata("unknown")).thenReturn(Optional.empty());

        ResponseEntity<IntegrationWithConfigResponse> response = controller.getByType(request, "unknown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetConfigFound() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");
        when(integrationFacade.getConfig(1L, "jira")).thenReturn(Optional.of(config));

        ResponseEntity<Map<String, Object>> response = controller.getConfig(request, "jira");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("abc", response.getBody().get("token"));
    }

    @Test
    void testGetConfigProviderNotFound() throws Exception {
        when(integrationFacade.getProvider("unknown")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getConfig(request, "unknown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetConfigNoConfigStored() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(integrationFacade.getConfig(1L, "jira")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getConfig(request, "jira");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testPutConfigSuccess() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(integrationFacade.putConfig(eq(1L), eq("jira"), any())).thenReturn(true);

        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");

        ResponseEntity<Void> response = controller.putConfig(request, "jira", config);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testPutConfigProviderNotFound() throws Exception {
        when(integrationFacade.getProvider("unknown")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.putConfig(request, "unknown", new HashMap<>());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testPutConfigValidationFails() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(integrationFacade.putConfig(eq(1L), eq("jira"), any())).thenReturn(false);

        ResponseEntity<Void> response = controller.putConfig(request, "jira", new HashMap<>());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testDeleteConfigSuccess() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        ResponseEntity<Void> response = controller.deleteConfig(request, "jira");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(integrationFacade).deleteConfig(1L, "jira");
    }

    @Test
    void testDeleteConfigProviderNotFound() throws Exception {
        when(integrationFacade.getProvider("unknown")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.deleteConfig(request, "unknown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testTestConnectionSuccess() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(integrationFacade.testConnection(1L, "jira")).thenReturn(true);

        ResponseEntity<Void> response = controller.testConnection(request, "jira");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testTestConnectionFails() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(integrationFacade.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(integrationFacade.testConnection(1L, "jira")).thenReturn(false);

        ResponseEntity<Void> response = controller.testConnection(request, "jira");
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void testTestConnectionProviderNotFound() throws Exception {
        when(integrationFacade.getProvider("unknown")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.testConnection(request, "unknown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testCreateProductBoard() throws Exception {
        assertNull(controller.createProductBoard(request, null));
    }
}
