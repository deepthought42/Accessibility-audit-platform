package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.services.AccountService;

@ExtendWith(MockitoExtension.class)
class BaseApiControllerTest {

    // Concrete subclass for testing the abstract class
    static class TestController extends BaseApiController {}

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TestController controller;

    @Test
    void testGetAuthenticatedAccountSuccess() throws UnknownAccountException {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("auth0|user123");
        Account account = new Account();
        account.setId(1L);
        when(accountService.findByUserId("user123")).thenReturn(account);

        Account result = controller.getAuthenticatedAccount(principal);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetAuthenticatedAccountThrowsWhenNotFound() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("auth0|unknown");
        when(accountService.findByUserId("unknown")).thenReturn(null);

        assertThrows(UnknownAccountException.class, () -> controller.getAuthenticatedAccount(principal));
    }

    @Test
    void testGetAuthenticatedAccountWithoutAuth0Prefix() throws UnknownAccountException {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user123");
        Account account = new Account();
        account.setId(2L);
        when(accountService.findByUserId("user123")).thenReturn(account);

        Account result = controller.getAuthenticatedAccount(principal);
        assertEquals(2L, result.getId());
    }

    @Test
    void testBuildPageableDefaults() {
        Pageable pageable = controller.buildPageable(0, 10, null);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void testBuildPageableNegativePage() {
        Pageable pageable = controller.buildPageable(-1, 10, "name");
        assertEquals(0, pageable.getPageNumber());
    }

    @Test
    void testBuildPageableSizeCappedAt100() {
        Pageable pageable = controller.buildPageable(0, 200, null);
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    void testBuildPageableSizeMinimum1() {
        Pageable pageable = controller.buildPageable(0, 0, null);
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    void testBuildPageableNegativeSize() {
        Pageable pageable = controller.buildPageable(0, -5, null);
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    void testBuildSortDefault() {
        Sort sort = controller.buildSort(null);
        assertNotNull(sort);
        Sort.Order order = sort.iterator().next();
        assertEquals("createdAt", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void testBuildSortEmptyString() {
        Sort sort = controller.buildSort("");
        assertNotNull(sort);
        Sort.Order order = sort.iterator().next();
        assertEquals("createdAt", order.getProperty());
    }

    @Test
    void testBuildSortWithPropertyOnly() {
        Sort sort = controller.buildSort("name");
        Sort.Order order = sort.iterator().next();
        assertEquals("name", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void testBuildSortWithPropertyAndDesc() {
        Sort sort = controller.buildSort("name,desc");
        Sort.Order order = sort.iterator().next();
        assertEquals("name", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void testBuildSortWithPropertyAndAsc() {
        Sort sort = controller.buildSort("title,asc");
        Sort.Order order = sort.iterator().next();
        assertEquals("title", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void testBuildSortCaseInsensitiveDirection() {
        Sort sort = controller.buildSort("name,DESC");
        Sort.Order order = sort.iterator().next();
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }
}
