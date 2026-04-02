package com.looksee.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import com.looksee.models.rules.RuleType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Unit tests for all exception classes.
 */
class ExceptionClassesTest {

    // ===== AuditCreationException =====
    @Test
    void auditCreationExceptionDefaultMessage() {
        AuditCreationException e = new AuditCreationException();
        assertEquals("There was an unexpected error while starting the audit. Please try again.", e.getMessage());
    }

    @Test
    void auditCreationExceptionCustomMessage() {
        AuditCreationException e = new AuditCreationException("custom");
        assertEquals("custom", e.getMessage());
    }

    @Test
    void auditCreationExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root");
        AuditCreationException e = new AuditCreationException("msg", cause);
        assertEquals("msg", e.getMessage());
        assertEquals(cause, e.getCause());
    }

    // ===== Auth0ManagementApiException =====
    @Test
    void auth0ManagementApiExceptionMessage() {
        Auth0ManagementApiException e = new Auth0ManagementApiException();
        assertEquals("An error occurred while updating user account", e.getMessage());
    }

    @Test
    void auth0ManagementApiExceptionStatus() {
        ResponseStatus status = Auth0ManagementApiException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, status.code());
    }

    // ===== DiscoveryStoppedException =====
    @Test
    void discoveryStoppedExceptionDefaultMessage() {
        DiscoveryStoppedException e = new DiscoveryStoppedException();
        assertEquals("Discovery has been stopped", e.getMessage());
    }

    @Test
    void discoveryStoppedExceptionCustomMessage() {
        DiscoveryStoppedException e = new DiscoveryStoppedException("custom msg");
        assertEquals("custom msg", e.getMessage());
    }

    @Test
    void discoveryStoppedExceptionStatus() {
        ResponseStatus status = DiscoveryStoppedException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.CONFLICT, status.value());
    }

    // ===== DomainNotOwnedByAccountException =====
    @Test
    void domainNotOwnedByAccountExceptionMessage() {
        DomainNotOwnedByAccountException e = new DomainNotOwnedByAccountException();
        assertEquals("Domain is not owned by your account", e.getMessage());
    }

    @Test
    void domainNotOwnedByAccountExceptionStatus() {
        ResponseStatus status = DomainNotOwnedByAccountException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status.code());
    }

    // ===== ExistingRuleException =====
    @Test
    void existingRuleExceptionMessage() {
        ExistingRuleException e = new ExistingRuleException("PATTERN");
        assertEquals("Element already has the PATTERN rule applied.", e.getMessage());
    }

    @Test
    void existingRuleExceptionIsRuntime() {
        assertTrue(RuntimeException.class.isAssignableFrom(ExistingRuleException.class));
    }

    // ===== FreeTrialExpiredException =====
    @Test
    void freeTrialExpiredExceptionMessage() {
        FreeTrialExpiredException e = new FreeTrialExpiredException();
        assertEquals("Your free trial has ended. Sign up for a plan.", e.getMessage());
    }

    @Test
    void freeTrialExpiredExceptionIsRuntime() {
        assertTrue(RuntimeException.class.isAssignableFrom(FreeTrialExpiredException.class));
    }

    @Test
    void freeTrialExpiredExceptionStatus() {
        ResponseStatus status = FreeTrialExpiredException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.CONFLICT, status.value());
    }

    // ===== InsufficientSubscriptionException =====
    @Test
    void insufficientSubscriptionExceptionMessage() {
        InsufficientSubscriptionException e = new InsufficientSubscriptionException();
        assertEquals("Upgrade your account to run a competitive analysis", e.getMessage());
    }

    @Test
    void insufficientSubscriptionExceptionStatus() {
        ResponseStatus status = InsufficientSubscriptionException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.CONFLICT, status.value());
    }

    // ===== InvalidApiKeyException =====
    @Test
    void invalidApiKeyExceptionDefaultMessage() {
        InvalidApiKeyException e = new InvalidApiKeyException();
        assertEquals("Invalid API key", e.getMessage());
    }

    @Test
    void invalidApiKeyExceptionCustomMessage() {
        InvalidApiKeyException e = new InvalidApiKeyException("bad key");
        assertEquals("bad key", e.getMessage());
    }

    @Test
    void invalidApiKeyExceptionStatus() {
        ResponseStatus status = InvalidApiKeyException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.PAYMENT_REQUIRED, status.value());
    }

    // ===== InvalidUserException =====
    @Test
    void invalidUserExceptionMessage() {
        InvalidUserException e = new InvalidUserException();
        assertEquals("Invalid user", e.getMessage());
    }

    @Test
    void invalidUserExceptionStatus() {
        ResponseStatus status = InvalidUserException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status.code());
    }

    // ===== MissingSubscriptionException =====
    @Test
    void missingSubscriptionExceptionMessage() {
        MissingSubscriptionException e = new MissingSubscriptionException();
        assertEquals("Welcome to Look-see! Sign up for a plan to get started.", e.getMessage());
    }

    @Test
    void missingSubscriptionExceptionIsRuntime() {
        assertTrue(RuntimeException.class.isAssignableFrom(MissingSubscriptionException.class));
    }

    // ===== PagesAreNotMatchingException =====
    @Test
    void pagesAreNotMatchingExceptionMessage() {
        PagesAreNotMatchingException e = new PagesAreNotMatchingException();
        assertEquals("Expected page and actual page did not match.", e.getMessage());
    }

    // ===== PaymentDueException =====
    @Test
    void paymentDueExceptionDefaultMessage() {
        PaymentDueException e = new PaymentDueException();
        assertEquals("Make a payment to continue using Look-see", e.getMessage());
    }

    @Test
    void paymentDueExceptionCustomMessage() {
        PaymentDueException e = new PaymentDueException("custom");
        assertEquals("custom", e.getMessage());
    }

    @Test
    void paymentDueExceptionStatus() {
        ResponseStatus status = PaymentDueException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.PAYMENT_REQUIRED, status.value());
    }

    // ===== RuleValueRequiredException =====
    @Test
    void ruleValueRequiredExceptionMessage() {
        RuleValueRequiredException e = new RuleValueRequiredException(RuleType.PATTERN);
        assertTrue(e.getMessage().contains("pattern"));
        assertTrue(e.getMessage().contains("requires a value"));
    }

    // ===== ServiceUnavailableException =====
    @Test
    void serviceUnavailableExceptionMessage() {
        ServiceUnavailableException e = new ServiceUnavailableException("service down");
        assertEquals("service down", e.getMessage());
    }

    @Test
    void serviceUnavailableExceptionIsRuntime() {
        assertTrue(RuntimeException.class.isAssignableFrom(ServiceUnavailableException.class));
    }

    // ===== SubscriptionExceededException =====
    @Test
    void subscriptionExceededExceptionDefaultMessage() {
        SubscriptionExceededException e = new SubscriptionExceededException();
        assertTrue(e.getMessage().contains("exceeded"));
    }

    @Test
    void subscriptionExceededExceptionCustomMessage() {
        SubscriptionExceededException e = new SubscriptionExceededException("limit hit");
        assertEquals("limit hit", e.getMessage());
    }

    // ===== UnknownAccountException =====
    @Test
    void unknownAccountExceptionStatus() {
        ResponseStatus status = UnknownAccountException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.FORBIDDEN, status.code());
        assertTrue(status.reason().contains("Unable to find account"));
    }
}
