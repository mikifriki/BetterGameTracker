package com.bettergametracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class ApiIntegrationTests {

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
    private static final String REVIEW = """
            {"reviewDate":"2026-09-08","reviewTitle":"Original","review":"Enjoyed it","rating":9}
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"missing", "null", "blank"})
    void persistsOptionalGameDetailsAndPlaytimeOnCreateAndUpdate(String variant) throws Exception {
        var gameBody = objectMapper.readTree(GAME);
        var playBody = objectMapper.readTree(PLAY);
        for (var body : java.util.List.of(gameBody, playBody)) {
            var fields = body == gameBody
                    ? java.util.List.of("description", "releasePlatform", "developer")
                    : java.util.List.of("location");
            for (String field : fields) {
                var object = (com.fasterxml.jackson.databind.node.ObjectNode) body;
                switch (variant) {
                    case "missing" -> object.remove(field);
                    case "null" -> object.putNull(field);
                    default -> object.put(field, "");
                }
            }
        }
        String game = create("/api/v1/games", gameBody.toString());
        String play = create(game + "/plays", playBody.toString());
        try {
            for (int round = 0; round < 2; round++) {
                var loadedGame = objectMapper.readTree(mockMvc.perform(get(game)).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
                for (String field : java.util.List.of("description", "releasePlatform", "developer")) {
                    assertThat(loadedGame.path(field)).isEqualTo(variant.equals("blank")
                            ? objectMapper.getNodeFactory().textNode("") : objectMapper.getNodeFactory().nullNode());
                }
                mockMvc.perform(get(play)).andExpect(status().isOk())
                        .andExpect(jsonPath("$.location").value(variant.equals("blank")
                                ? org.hamcrest.Matchers.equalTo("") : org.hamcrest.Matchers.nullValue()));
                if (round == 0) {
                    mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON).content(GAME))
                            .andExpect(status().isOk());
                    mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(PLAY))
                            .andExpect(status().isOk());
                    mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON).content(gameBody.toString()))
                            .andExpect(status().isOk());
                    mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(playBody.toString()))
                            .andExpect(status().isOk());
                }
            }
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void persistsTypedGameFieldsAndOptionalPlayFields() throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(GAME);
        body.put("description", "d".repeat(1000));
        body.put("releaseDate", "2024-02-29");
        body.put("physicalCopy", true);
        String game = create("/api/v1/games", body.toString());
        String play = create(game + "/plays", "{}");
        try {
            mockMvc.perform(get(game)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("d".repeat(1000)))
                    .andExpect(jsonPath("$.releaseDate").value("2024-02-29"))
                    .andExpect(jsonPath("$.physicalCopy").value(true));
            for (String field : java.util.List.of("playthroughRating", "completionDate", "platformPlayedOn",
                    "timeToBeatMinutes", "completionStatus", "location", "coop")) {
                mockMvc.perform(get(play)).andExpect(status().isOk())
                        .andExpect(jsonPath("$." + field).value(org.hamcrest.Matchers.nullValue()));
            }
            body.put("physicalCopy", false);
            mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isOk());
            mockMvc.perform(get(game)).andExpect(jsonPath("$.physicalCopy").value(false));
            for (boolean coop : new boolean[] {true, false}) {
                mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"coop\":" + coop + "}"))
                        .andExpect(status().isOk());
                mockMvc.perform(get(play)).andExpect(jsonPath("$.coop").value(coop));
            }
            mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(PLAY))
                    .andExpect(status().isOk());
            mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content("""
                    {"playthroughRating":null,"completionDate":null,"platformPlayedOn":null,
                     "timeToBeatMinutes":null,"completionStatus":null,"location":null,"coop":null}
                    """)).andExpect(status().isOk());
            mockMvc.perform(get(play)).andExpect(jsonPath("$.location").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.coop").value(org.hamcrest.Matchers.nullValue()));
            mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"gameTitle\":\"Game\",\"releaseDate\":null,\"physicalCopy\":null}"))
                    .andExpect(status().isOk());
            mockMvc.perform(get(game))
                    .andExpect(jsonPath("$.releaseDate").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.physicalCopy").value(org.hamcrest.Matchers.nullValue()));
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void persistsCompletionStatusesAndRejectsInvalidValues() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", "{}");
        try {
            for (var completionStatus : com.bettergametracker.play.CompletionStatus.values()) {
                String body = "{\"completionStatus\":\"" + completionStatus + "\"}";
                String created = create(game + "/plays", body);
                mockMvc.perform(get(created))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.completionStatus").value(completionStatus.name()))
                        .andExpect(jsonPath("$.completionRate").doesNotExist());
                mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk());
                mockMvc.perform(get(play)).andExpect(jsonPath("$.completionStatus").value(completionStatus.name()));
            }
            for (String value : java.util.List.of("\"100%\"", "\"UNKNOWN\"", "0")) {
                for (String method : java.util.List.of("POST", "PUT")) {
                    mockMvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? game + "/plays" : play)
                                    .contentType(MediaType.APPLICATION_JSON).content("{\"completionStatus\":" + value + "}"))
                            .andExpect(status().isBadRequest())
                            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                }
            }
            for (String body : java.util.List.of("{\"completionStatus\":null}", "{}")) {
                mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk());
                mockMvc.perform(get(play))
                        .andExpect(jsonPath("$.completionStatus").value(org.hamcrest.Matchers.nullValue()));
            }
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void rejectsInvalidDatesAndBooleanValues() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", "{}");
        try {
            for (String date : java.util.List.of("2026", "not-a-date", "2026-02-30", "2026-09-09T12:00:00")) {
                var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(GAME);
                body.put("releaseDate", date);
                for (String method : java.util.List.of("POST", "PUT")) {
                    mockMvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/api/v1/games" : game)
                                    .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
                }
            }
            for (String value : java.util.List.of("\"Yes\"", "{}", "[]")) {
                mockMvc.perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"gameTitle\":\"Game\",\"physicalCopy\":" + value + "}"))
                        .andExpect(status().isBadRequest());
                mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content("{\"coop\":" + value + "}"))
                        .andExpect(status().isBadRequest());
            }
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void enforcesTextLimitsOnCreateAndUpdate() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String review = create(play + "/reviews", REVIEW);
        String time = create(play + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":1}");
        try {
            for (String resource : java.util.List.of(game, play, review, time)) {
                String original = resource.equals(game) ? GAME : resource.equals(play) ? PLAY
                        : resource.equals(review) ? REVIEW : "{\"date\":\"2026-09-09\",\"durationMinutes\":1,\"notes\":\"Notes\"}";
                var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(original);
                var fields = new java.util.ArrayList<String>();
                body.fieldNames().forEachRemaining(fields::add);
                for (String field : fields) {
                    if (!body.get(field).isTextual() || field.equals("releaseDate") || field.equals("date") || field.equals("completionStatus")
                            || field.equals("completionDate") || field.equals("reviewDate")) {
                        continue;
                    }
                    int limit = field.equals("description") ? 1000 : field.equals("review") ? 5000 : 255;
                    for (String method : java.util.List.of("POST", "PUT")) {
                        String path = method.equals("POST") ? resource.substring(0, resource.lastIndexOf('/')) : resource;
                        var requestBody = body.deepCopy().put(field, "x".repeat(limit));
                        var response = mockMvc.perform(request(HttpMethod.valueOf(method), path)
                                        .contentType(MediaType.APPLICATION_JSON).content(requestBody.toString()))
                                .andExpect(status().is(method.equals("POST") ? 201 : 200));
                        if (method.equals("POST")) {
                            mockMvc.perform(delete(response.andReturn().getResponse().getHeader("Location")))
                                    .andExpect(status().isNoContent());
                        }
                        requestBody.put(field, "x".repeat(limit + 1));
                        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                                        .contentType(MediaType.APPLICATION_JSON).content(requestBody.toString()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors." + field).exists());
                    }
                }
            }
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void requiresGameTitleOnCreateAndUpdate() throws Exception {
        String game = create("/api/v1/games", GAME);
        try {
            for (String body : java.util.List.of("{}", "{\"gameTitle\":null}", "{\"gameTitle\":\"   \"}")) {
                mockMvc.perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest());
                mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest());
            }
        } finally {
            mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void commitsCrudAcrossRequestsAndPreservesUnrelatedRecords() throws Exception {
        String game = create("/api/v1/games", GAME);
        String play = create(game + "/plays", PLAY);
        String review = create(play + "/reviews", REVIEW);
        String otherGame = create("/api/v1/games", GAME);
        String otherPlay = create(otherGame + "/plays", PLAY);
        String otherReview = create(otherPlay + "/reviews", REVIEW);

        mockMvc.perform(get(game)).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameTitle").value("Game"));
        mockMvc.perform(get(play)).andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Home"));
        mockMvc.perform(get(review)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewTitle").value("Original"));
        mockMvc.perform(get(game + "/plays")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(play + "/reviews")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put(game).contentType(MediaType.APPLICATION_JSON)
                        .content(GAME.replace("\"Game\"", "\"Updated\"")))
                .andExpect(status().isOk());
        mockMvc.perform(put(play).contentType(MediaType.APPLICATION_JSON).content(PLAY.replace("Home", "Away")))
                .andExpect(status().isOk());
        mockMvc.perform(put(review).contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW.replace("Original", "Updated")))
                .andExpect(status().isOk());
        mockMvc.perform(get(game)).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameTitle").value("Updated"));
        mockMvc.perform(get(play)).andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Away"));
        mockMvc.perform(get(review)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewTitle").value("Updated"));

        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE}) {
            mockMvc.perform(request(method, play.replace(game, otherGame))
                            .contentType(MediaType.APPLICATION_JSON).content(PLAY))
                    .andExpect(status().isNotFound());
            mockMvc.perform(request(method, review.replace(play, otherPlay))
                            .contentType(MediaType.APPLICATION_JSON).content(REVIEW))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get(review)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewTitle").value("Updated"));

        mockMvc.perform(delete(review)).andExpect(status().isNoContent());
        mockMvc.perform(get(review)).andExpect(status().isNotFound());
        create(play + "/reviews", REVIEW);
        mockMvc.perform(delete(play)).andExpect(status().isNoContent());
        mockMvc.perform(get(play)).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isEqualTo(1);

        String replacementPlay = create(game + "/plays", PLAY);
        create(replacementPlay + "/reviews", REVIEW);
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
        mockMvc.perform(get(game)).andExpect(status().isNotFound());
        for (String table : new String[] {"games", "play_entries", "reviews"}) {
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
        }
        mockMvc.perform(get(otherGame)).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameTitle").value("Game"));
        mockMvc.perform(get(otherPlay)).andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Home"));
        mockMvc.perform(get(otherReview)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewTitle").value("Original"));
    }

    @Test
    void storesReplacesAndDeletesCoversAndRejectsUnsafeUploads() throws Exception {
        String game = create("/api/v1/games", GAME);
        String cover = game + "/cover";
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD3sAAAAASUVORK5CYII=");
        mockMvc.perform(get(cover)).andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Cover not found"))
                .andExpect(jsonPath("$.instance").value(cover));
        mockMvc.perform(multipart(cover).file(new org.springframework.mock.web.MockMultipartFile(
                        "file", "../../outside.png", "image/png", png))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(cover)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
        for (byte[] bytes : new byte[][] {"<svg onload='alert(1)'/>".getBytes(), new byte[0], new byte[6 * 1024 * 1024]}) {
            mockMvc.perform(multipart(cover).file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "fake.png", "image/png", bytes))
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().is(bytes.length > 5 * 1024 * 1024 ? 413 : 415))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(bytes.length > 5 * 1024 * 1024 ? 413 : 415))
                    .andExpect(jsonPath("$.title").value(bytes.length > 5 * 1024 * 1024
                            ? "Payload Too Large" : "Unsupported Media Type"))
                    .andExpect(jsonPath("$.detail").value(bytes.length > 5 * 1024 * 1024
                            ? "Cover must be at most 5 MB" : "Upload a PNG or JPEG image"))
                    .andExpect(jsonPath("$.instance").value(cover));
            mockMvc.perform(get(cover)).andExpect(content().bytes(png));
        }
        mockMvc.perform(multipart(cover).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").value(cover));
        mockMvc.perform(get(cover)).andExpect(content().bytes(png));
        mockMvc.perform(delete(cover)).andExpect(status().isNoContent());
        mockMvc.perform(get(cover)).andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Cover not found"))
                .andExpect(jsonPath("$.instance").value(cover));
        mockMvc.perform(delete(game)).andExpect(status().isNoContent());
    }

    @Test
    void rejectsForeignOriginsAndHostnamesInLocalMode() throws Exception {
        mockMvc.perform(delete("/api/v1/games/" + java.util.UUID.randomUUID())
                        .header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.instance").isString());
        mockMvc.perform(get("/api/v1/games").with(request -> {
            request.setServerName("untrusted.example"); return request;
        })).andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.instance").isString());
        mockMvc.perform(get("/")).andExpect(status().isOk());
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
            Path path = Files.createTempFile("better-game-tracker-api-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}
