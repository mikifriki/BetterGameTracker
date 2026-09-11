package com.bettergametracker.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.flyway.placeholders.uuidType=BLOB",
        "spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON",
        "spring.datasource.hikari.maximum-pool-size=1",
        "BETTER_GAME_TRACKER_GOOGLE_CLIENT_ID=test-client",
        "BETTER_GAME_TRACKER_GOOGLE_CLIENT_SECRET=test-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("hosted")
class HostedSecurityIntegrationTests {
    private static final Path DIRECTORY = temporaryDirectory();
    private static final String GAME = """
            {"gameTitle":"Private game","description":"A game","releasePlatform":"PC",
             "releaseDate":"2026-01-01","developer":"Studio","metaRating":9,"userRating":9,"physicalCopy":false}
            """;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ApplicationUserRepository users;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String postgres = System.getenv("BGT_TEST_POSTGRES_URL");
        boolean usePostgres = postgres != null && !postgres.isBlank();
        registry.add("spring.datasource.url", () -> usePostgres ? postgres : "jdbc:sqlite:" + DIRECTORY.resolve("hosted.db"));
        if (usePostgres) {
            registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.flyway.placeholders.uuidType", () -> "UUID");
            registry.add("spring.datasource.hikari.connection-init-sql", () -> "SELECT 1");
        }
        registry.add("spring.datasource.username", () -> usePostgres ? System.getenv("BGT_TEST_POSTGRES_USERNAME") : "");
        registry.add("spring.datasource.password", () -> usePostgres ? System.getenv("BGT_TEST_POSTGRES_PASSWORD") : "");
        registry.add("better-game-tracker.cover-directory", () -> DIRECTORY.resolve("covers").toString());
    }

    @Test
    void requiresLoginAndCsrfAndExposesOnlySessionMetadata() throws Exception {
        mvc.perform(get("/api/v1/games")).andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Sign in required"))
                .andExpect(jsonPath("$.instance").value("/api/v1/games"));
        mvc.perform(get("/api/v1/session")).andExpect(status().isOk())
                .andExpect(jsonPath("$.hosted").value(true))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.csrfToken").isString());
        mvc.perform(post("/api/v1/games").with(oidcLogin()).contentType(MediaType.APPLICATION_JSON).content(GAME))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.instance").value("/api/v1/games"));
        mvc.perform(post("/api/v1/games").with(oidcLogin()).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON).content(GAME))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.instance").value("/api/v1/games"));
        mvc.perform(get("/private").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/oauth2/authorization/google")));
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void isolatesGamesAndAllNestedResourcesBetweenUsers() throws Exception {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        users.register(UUID.randomUUID(), first);
        UUID internalId = users.findByGoogleSubject(first).orElseThrow().getId();
        users.register(UUID.randomUUID(), first);
        assertThat(users.findByGoogleSubject(first).orElseThrow().getId()).isEqualTo(internalId);
        users.register(UUID.randomUUID(), second);
        String game = create("/api/v1/games", GAME, first);
        String play = create(game + "/plays", """
                {"playthroughRating":9,"completionDate":"2026-01-01","platformPlayedOn":"PC",
                 "timeToBeatMinutes":30,"completionStatus":"COMPLETE","location":"Home"}
                """, first);
        String review = create(play + "/reviews", "{}", first);
        String time = create(play + "/time-entries", "{\"date\":\"2026-09-08\",\"durationMinutes\":30}", first);
        mvc.perform(get("/api/v1/games").with(oidcLogin().idToken(t -> t.subject(second))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        for (String resource : new String[] {game, play, review, time, game + "/cover"}) {
            mvc.perform(delete(resource).with(oidcLogin().idToken(t -> t.subject(second))).with(csrf()))
                    .andExpect(status().isNotFound());
        }
        for (String resource : new String[] {game, play, review, play + "/time-entries", game + "/cover"}) {
            mvc.perform(get(resource).with(oidcLogin().idToken(t -> t.subject(second))))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(put(game).with(oidcLogin().idToken(t -> t.subject(second))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(GAME)).andExpect(status().isNotFound());
        mvc.perform(post(game + "/plays").with(oidcLogin().idToken(t -> t.subject(second))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"playthroughRating":9,"completionDate":"2026-01-01","platformPlayedOn":"PC",
                         "timeToBeatMinutes":30,"completionStatus":"COMPLETE","location":"Home"}
                        """)).andExpect(status().isNotFound());
        mvc.perform(multipart(game + "/cover").file(new MockMultipartFile("file", "x.png", "image/png", new byte[8]))
                .with(request -> { request.setMethod("PUT"); return request; })
                .with(oidcLogin().idToken(t -> t.subject(second))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(game).with(oidcLogin().idToken(t -> t.subject(first))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").doesNotExist());
        mvc.perform(delete(game).with(oidcLogin().idToken(t -> t.subject(first))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private String create(String path, String body, String subject) throws Exception {
        return mvc.perform(post(path).with(oidcLogin().idToken(t -> t.subject(subject))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    private static Path temporaryDirectory() {
        try { return Files.createTempDirectory("bgt-hosted-test-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
}
