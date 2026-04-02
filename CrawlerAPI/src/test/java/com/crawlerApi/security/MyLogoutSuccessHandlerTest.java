package com.crawlerApi.security;

import static org.mockito.Mockito.*;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class MyLogoutSuccessHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpSession session;

    @Test
    void testOnLogoutSuccessRemovesUserAttribute() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        MyLogoutSuccessHandler handler = new MyLogoutSuccessHandler();
        handler.onLogoutSuccess(request, response, authentication);

        verify(session).removeAttribute("user");
    }

    @Test
    void testOnLogoutSuccessWithNullSession() throws IOException, ServletException {
        when(request.getSession()).thenReturn(null);

        MyLogoutSuccessHandler handler = new MyLogoutSuccessHandler();
        // Should not throw when session is null
        handler.onLogoutSuccess(request, response, authentication);
    }

    @Test
    void testOnLogoutSuccessWithNullAuthentication() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        MyLogoutSuccessHandler handler = new MyLogoutSuccessHandler();
        handler.onLogoutSuccess(request, response, null);

        verify(session).removeAttribute("user");
    }
}
