package com.bettergametracker.security;

import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final boolean hosted;
    private final ApplicationUserRepository users;

    public CurrentUser(Environment environment,
            ApplicationUserRepository users) {
        this.hosted = environment.acceptsProfiles(Profiles.of("hosted"));
        this.users = users;
    }

    public UUID id() {
        if (!hosted) {
            return null;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser user)) {
            throw new AuthenticationCredentialsNotFoundException("Sign in required");
        }
        return users.findByGoogleSubject(user.getSubject()).orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException("Application user not registered")).getId();
    }
}
