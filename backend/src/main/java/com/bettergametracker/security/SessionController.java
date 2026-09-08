package com.bettergametracker.security;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SessionController {
    private final boolean hosted;

    public SessionController(Environment environment) {
        this.hosted = environment.acceptsProfiles(Profiles.of("hosted"));
    }

    @GetMapping("/api/v1/session")
    public SessionResponse session(Authentication authentication, HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        boolean authenticated = authentication != null && authentication.getPrincipal() instanceof OidcUser;
        return new SessionResponse(hosted, authenticated, csrf == null ? null : csrf.getToken(),
                csrf == null ? null : csrf.getHeaderName());
    }

    public record SessionResponse(boolean hosted, boolean authenticated, String csrfToken, String csrfHeader) {}
}
