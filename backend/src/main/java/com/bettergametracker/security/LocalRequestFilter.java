package com.bettergametracker.security;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reject browser requests from other sites and DNS rebinding against the unauthenticated local app. */
public class LocalRequestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        String expectedOrigin = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 ? "" : ":" + request.getServerPort());
        if (!Set.of("localhost", "127.0.0.1", "[::1]").contains(request.getServerName())
                || (origin != null && !origin.equals(expectedOrigin))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
