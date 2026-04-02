package com.crawlerApi.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleCORSFilterTest {

    @Mock
    private ServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    void testDoFilterSetsCorsHeaders() throws IOException, ServletException {
        SimpleCORSFilter filter = new SimpleCORSFilter();
        filter.doFilter(request, response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "*");
        verify(response).setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE, PATCH");
        verify(response).setHeader("Access-Control-Max-Age", "3600");
        verify(response).setHeader(eq("Access-Control-Allow-Headers"), anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterChainsRequest() throws IOException, ServletException {
        SimpleCORSFilter filter = new SimpleCORSFilter();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void testInitDoesNotThrow() {
        SimpleCORSFilter filter = new SimpleCORSFilter();
        assertDoesNotThrow(() -> filter.init(null));
    }

    @Test
    void testDestroyDoesNotThrow() {
        SimpleCORSFilter filter = new SimpleCORSFilter();
        assertDoesNotThrow(filter::destroy);
    }
}
