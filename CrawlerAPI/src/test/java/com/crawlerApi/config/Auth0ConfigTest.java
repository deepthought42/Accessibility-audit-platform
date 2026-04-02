package com.crawlerApi.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Auth0ConfigTest {

    private Auth0Config config;

    @BeforeEach
    void setUp() {
        config = new Auth0Config();
    }

    @Test
    void testSetAndGetDomain() {
        config.setDomain("test.auth0.com");
        assertEquals("test.auth0.com", config.getDomain());
        assertEquals("test.auth0.com", config.getAuth0Domain());
    }

    @Test
    void testSetAndGetIssuer() {
        config.setIssuer("https://test.auth0.com/");
        assertEquals("https://test.auth0.com/", config.getIssuer());
        assertEquals("https://test.auth0.com/", config.getAuth0Issuer());
    }

    @Test
    void testSetAndGetApiAudience() {
        config.setApiAudience("https://api.example.com");
        assertEquals("https://api.example.com", config.getApiAudience());
        assertEquals("https://api.example.com", config.getAuth0ApiAudience());
    }

    @Test
    void testSetAndGetClientId() {
        config.setClientId("clientId123");
        assertEquals("clientId123", config.getClientId());
        assertEquals("clientId123", config.getAuth0ClientId());
    }

    @Test
    void testSetAndGetClientSecret() {
        config.setClientSecret("secret123");
        assertEquals("secret123", config.getClientSecret());
        assertEquals("secret123", config.getAuth0ClientSecret());
    }

    @Test
    void testSetAndGetAudience() {
        config.setAudience("https://audience.example.com");
        assertEquals("https://audience.example.com", config.getAudience());
        assertEquals("https://audience.example.com", config.getAuth0Audience());
    }

    @Test
    void testSetAndGetSecuredRoute() {
        config.setSecuredRoute("/api/secured");
        assertEquals("/api/secured", config.getSecuredRoute());
    }

    @Test
    void testSetAndGetBase64EncodedSecret() {
        config.setBase64EncodedSecret(true);
        assertTrue(config.isBase64EncodedSecret());

        config.setBase64EncodedSecret(false);
        assertFalse(config.isBase64EncodedSecret());
    }

    @Test
    void testSetAndGetAuthorityStrategy() {
        config.setAuthorityStrategy("ROLES");
        assertEquals("ROLES", config.getAuthorityStrategy());
    }

    @Test
    void testSetAndGetDefaultAuth0ApiSecurityEnabled() {
        config.setDefaultAuth0ApiSecurityEnabled(true);
        assertTrue(config.isDefaultAuth0ApiSecurityEnabled());
    }

    @Test
    void testSetAndGetSigningAlgorithm() {
        config.setSigningAlgorithm("RS256");
        assertEquals("RS256", config.getSigningAlgorithm());
    }

    @Test
    void testDefaultValuesAreNull() {
        assertNull(config.getDomain());
        assertNull(config.getIssuer());
        assertNull(config.getApiAudience());
        assertNull(config.getClientId());
        assertNull(config.getClientSecret());
        assertNull(config.getAudience());
    }
}
