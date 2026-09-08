package com.bettergametracker.security;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfiguration {
    @Bean
    @Profile("local")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http.addFilterBefore(new LocalRequestFilter(), AuthorizationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")))
                .build();
    }

    @Bean
    @Profile("hosted")
    SecurityFilterChain hostedSecurity(HttpSecurity http, ApplicationUserRepository users) throws Exception {
        OidcUserService delegate = new OidcUserService();
        return http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/api/v1/session", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var user = delegate.loadUser(request);
                    users.register(UUID.randomUUID(), user.getSubject());
                    return user;
                })).defaultSuccessUrl("/", true))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")))
                .build();
    }
}
