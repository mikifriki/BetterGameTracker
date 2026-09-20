package com.bettergametracker.security;

import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bettergametracker.api.ApiProblemWriter;
import com.bettergametracker.config.LocalNetworkConfiguration;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

@Configuration
public class SecurityConfiguration {
    @Bean
    ApiProblemWriter apiProblemWriter(ObjectMapper mapper) {
        return new ApiProblemWriter(mapper);
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localSecurity(HttpSecurity http, ApiProblemWriter problems,
            LocalNetworkConfiguration network) throws Exception {
        return http.addFilterBefore(new LocalRequestFilter(problems, network), CsrfFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> { if (!network.isLanEnabled()) csrf.disable(); })
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        problems.write(request, response, HttpStatus.FORBIDDEN,
                                "Invalid or expired CSRF token. Reload the page and try again.")))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")))
                .build();
    }

    @Bean
    @Profile("hosted")
    SecurityFilterChain hostedSecurity(HttpSecurity http, ApplicationUserRepository users, ApiProblemWriter problems) throws Exception {
        OidcUserService delegate = new OidcUserService();
        var api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        var browserAccessDenied = new AccessDeniedHandlerImpl();
        return http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/index.html", "/*.js", "/*.css", "/media/**").permitAll()
                        .requestMatchers("/", "/library", "/stats", "/settings", "/help", "/about", "/games/**",
                                "/api/v1/session", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var user = delegate.loadUser(request);
                    users.register(UUID.randomUUID(), user.getSubject());
                    return user;
                })).defaultSuccessUrl("/", true))
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor((request, response, exception) ->
                                problems.write(request, response, HttpStatus.UNAUTHORIZED, "Sign in required"), api)
                        .accessDeniedHandler((request, response, exception) -> {
                            if (api.matches(request)) {
                                problems.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
                            } else {
                                browserAccessDenied.handle(request, response, exception);
                            }
                        }))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")))
                .build();
    }
}
