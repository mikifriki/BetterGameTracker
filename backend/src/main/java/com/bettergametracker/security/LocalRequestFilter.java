package com.bettergametracker.security;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import com.bettergametracker.config.LocalNetworkConfiguration;
import com.bettergametracker.api.ApiProblemWriter;
import org.springframework.http.HttpStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reject browser requests from other sites and DNS rebinding against the unauthenticated local app. */
public class LocalRequestFilter extends OncePerRequestFilter {
    private final ApiProblemWriter problems;
    private final LocalNetworkConfiguration network;

    public LocalRequestFilter(ApiProblemWriter problems, LocalNetworkConfiguration network) {
        this.problems = problems;
        this.network = network;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        String host = request.getServerName();
        String expectedOrigin = request.getScheme() + "://" + request.getServerName()
                + ((request.isSecure() && request.getServerPort() == 443)
                        || (!request.isSecure() && request.getServerPort() == 80) ? "" : ":" + request.getServerPort());
        InetAddress remote = InetAddress.getByName(request.getRemoteAddr());
        boolean allowedHost = Set.of("localhost", "127.0.0.1", "[::1]").contains(host)
                || (network.isLanEnabled() && network.getAddress().equals(host));
        boolean allowedClient = remote.isLoopbackAddress()
                || (network.isLanEnabled() && remote.isSiteLocalAddress());
        if (!allowedHost || !allowedClient
                || (origin != null && !origin.equals(expectedOrigin))) {
            problems.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
            return;
        }
        chain.doFilter(request, response);
    }
}
