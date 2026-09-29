package com.bettergametracker.security;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LanSecurityIntegrationTests {
    private static final String BASE = "http://192.168.1.10:8080";
    private static final Path DATABASE = temporaryDatabase();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void sharesTheOwnerlessLibraryWithoutLoginAndRequiresSessionCsrfForWrites() throws Exception {
        var response = mvc.perform(get(URI.create(BASE + "/api/v1/session"))
                        .with(request -> { request.setRemoteAddr("192.168.1.20"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hosted").value(false))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.csrfToken").isString()).andReturn();
        var session = (MockHttpSession) response.getRequest().getSession(false);
        var csrf = mapper.readTree(response.getResponse().getContentAsString());
        String csrfHeader = csrf.get("csrfHeader").asText();
        String token = csrf.get("csrfToken").asText();
        for (String value : new String[] {"", "invalid"}) {
            mvc.perform(post(URI.create(BASE + "/api/v1/games")).session(session)
                            .header(csrfHeader, value).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"gameTitle\":\"Blocked\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
        String game = mvc.perform(post(URI.create(BASE + "/api/v1/games")).session(session)
                        .header("Origin", BASE).header(csrfHeader, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gameTitle\":\"Shared LAN game\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(URI.create(BASE + game))
                        .with(request -> { request.setRemoteAddr("192.168.1.21"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gameTitle").value("Shared LAN game"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM games WHERE owner_id IS NOT NULL", Integer.class)).isZero();
        var file = new MockMultipartFile("file", "cover.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        mvc.perform(multipart(URI.create(BASE + game + "/cover")).file(file).session(session)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(URI.create(BASE + game + "/cover")).file(file).session(session)
                        .header(csrfHeader, token).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isNoContent());
        mvc.perform(delete(URI.create(BASE + game)).session(session)).andExpect(status().isForbidden());
        mvc.perform(delete(URI.create(BASE + game)).session(session).header(csrfHeader, token))
                .andExpect(status().isNoContent());
        session.invalidate();
        mvc.perform(post(URI.create(BASE + "/api/v1/games")).header(csrfHeader, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gameTitle\":\"Expired\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "null", "http://192.168.1.10:8081"})
    void rejectsForeignOriginsEvenForSessionReads(String origin) throws Exception {
        mvc.perform(get(URI.create(BASE + "/api/v1/session")).header("Origin", origin))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.example", "203.0.113.11"})
    void rejectsForeignHosts(String host) throws Exception {
        mvc.perform(get(URI.create("http://" + host + ":8080/api/v1/session")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPublicClientsDespiteSpoofedForwardingHeaders() throws Exception {
        mvc.perform(get(URI.create(BASE + "/api/v1/session"))
                        .header("X-Forwarded-For", "192.168.1.20")
                        .header("Forwarded", "for=192.168.1.20")
                        .with(request -> { request.setRemoteAddr("203.0.113.20"); return request; }))
                .andExpect(status().isForbidden());
    }

    private static Path temporaryDatabase() {
        try { return Files.createTempDirectory("bgt-lan-test-").resolve("app.db"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
}
