package com.bettergametracker.play;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PlayTimeEntryApiIntegrationTests {
    @TestConfiguration
    static class CsrfRequests {
        @Bean
        MockMvcBuilderCustomizer csrfByDefault() {
            return builder -> builder.defaultRequest(get("/").with(csrf()));
        }
    }

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();
    private static final String GAME = """
            {"gameTitle":"Game","description":"Description","releasePlatform":"PC",
             "releaseDate":"2026-09-08","developer":"Developer","metaRating":9,
             "userRating":9,"physicalCopy":false}
            """;
    private static final String PLAY = """
            {"playthroughRating":9,"completionDate":"2026-09-08","platformPlayedOn":"PC",
             "timeToBeatMinutes":1200,"completionStatus":"COMPLETE","location":"Home"}
            """;
    private static final String TIME = """
            {"date":"2026-09-08","durationMinutes":30,"notes":"Evening session"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void temporarySqliteDatabase(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE_PATH::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
    }

    @Test
    void commitsCrudAndSameDateEntriesWithoutChangingOwnership() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String collection = play + "/time-entries";
        mockMvc.perform(get(collection)).andExpect(status().isOk()).andExpect(content().json("[]"));
        String first = create(collection, TIME);
        String second = create(collection, TIME);
        String otherPlay = create(game + "/plays", PLAY);
        String unrelated = create(otherPlay + "/time-entries", TIME);
        String playId = play.substring(play.lastIndexOf('/') + 1);
        mockMvc.perform(get(collection)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].playId").value(playId))
                .andExpect(jsonPath("$[0].date").value("2026-09-08"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].notes").value("Evening session"))
                .andExpect(jsonPath("$[0].playEntry").doesNotExist());
        mockMvc.perform(put(first).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-09-09","durationMinutes":60,"notes":null,"playId":"%s"}
                        """.formatted(otherPlay.substring(otherPlay.lastIndexOf('/') + 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playId").value(playId))
                .andExpect(jsonPath("$.date").value("2026-09-09"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.notes").doesNotExist());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT duration_minutes FROM play_time_entries WHERE duration_minutes = 60", Integer.class))
                .isEqualTo(60);
        mockMvc.perform(delete(first)).andExpect(status().isNoContent()).andExpect(content().string(""));
        for (HttpMethod method : new HttpMethod[] {HttpMethod.PUT, HttpMethod.DELETE}) {
            for (String target : new String[] {first, second.replace(play, otherPlay)}) {
                mockMvc.perform(request(method, target).contentType(MediaType.APPLICATION_JSON).content(TIME))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
            }
        }
        mockMvc.perform(get(collection)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(delete(play)).andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM play_time_entries", Integer.class)).isEqualTo(1);
        mockMvc.perform(delete(unrelated)).andExpect(status().isNoContent());
        mockMvc.perform(get(otherPlay)).andExpect(status().isOk());
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
    }

    @Test
    void rejectsMissingAndWrongParentsForEveryOperation() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String otherGame = create("/api/v1/games", GAME);
        for (String parent : new String[] {game + "/plays/" + java.util.UUID.randomUUID(),
                play.replace(game, otherGame), play.replace(game, "/api/v1/games/" + java.util.UUID.randomUUID())}) {
            for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
                String target = parent + "/time-entries";
                if (method == HttpMethod.PUT || method == HttpMethod.DELETE) {
                    target += "/" + java.util.UUID.randomUUID();
                }
                mockMvc.perform(request(method, target).contentType(MediaType.APPLICATION_JSON).content(TIME))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
            }
        }
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        mockMvc.perform(delete(otherGame)).andExpect(status().isNoContent());
    }

    @Test
    void validatesCreateAndUpdateAndPreservesExistingData() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String collection = play + "/time-entries";
        String entry = create(collection, TIME);
        for (HttpMethod method : new HttpMethod[] {HttpMethod.POST, HttpMethod.PUT}) {
            for (String body : new String[] {"{}", "{", "null", "[]",
                    TIME.replace("2026-09-08", "invalid"), TIME.replace("2026-09-08", "2026-02-30"),
                    TIME.replace("2026-09-08", "2026-09-08T12:30:00"),
                    TIME.replace("30", "1.5"), TIME.replace("30", "0"), TIME.replace("30", "-1"), TIME.replace("30", "null"),
                    TIME.replace("30", "2147483648"), TIME.replace("\"2026-09-08\"", "null")}) {
                mockMvc.perform(request(method, method == HttpMethod.POST ? collection : entry)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
            }
        }
        mockMvc.perform(get(collection)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].durationMinutes").value(30));
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
    }

    @Test
    void accepts5000CharacterNotesAndRejectsLongerNotesForCreateAndUpdate() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String collection = play + "/time-entries";
        String notes = "n".repeat(5000);
        String entry = create(collection, TIME.replace("Evening session", notes));
        String updatedNotes = "u".repeat(5000);
        mockMvc.perform(put(entry).contentType(MediaType.APPLICATION_JSON)
                        .content(TIME.replace("Evening session", updatedNotes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(updatedNotes));
        for (HttpMethod method : new HttpMethod[] {HttpMethod.POST, HttpMethod.PUT}) {
            mockMvc.perform(request(method, method == HttpMethod.POST ? collection : entry)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TIME.replace("Evening session", "n".repeat(5001))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get(collection)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].notes").value(updatedNotes));
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
    }

    private String create(String collection, String body) throws Exception {
        var response = mockMvc.perform(post(collection).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String location = collection + "/" + objectMapper.readTree(response.getContentAsString()).get("id").asText();
        assertThat(response.getHeader("Location")).isEqualTo(location);
        return location;
    }

    private static Path createTemporaryDatabasePath() {
        try {
            Path path = Files.createTempFile("better-game-tracker-time-api-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}
