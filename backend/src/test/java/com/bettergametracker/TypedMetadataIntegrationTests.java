package com.bettergametracker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bettergametracker.play.PlayEntryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
class TypedMetadataIntegrationTests {
    @TestConfiguration
    static class CsrfRequests {
        @Bean
        MockMvcBuilderCustomizer csrfByDefault() {
            return builder -> builder.defaultRequest(get("/").with(csrf()));
        }
    }
    private static final Path DIRECTORY = temporaryDirectory();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PlayEntryService plays;
    @Autowired EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String postgres = System.getenv("BGT_TEST_POSTGRES_URL");
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", () -> DIRECTORY.resolve("app.db").toString());
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DIRECTORY.resolve("app.db"));
        registry.add("better-game-tracker.cover-directory", () -> DIRECTORY.resolve("covers").toString());
        if (postgres != null && !postgres.isBlank()) {
            // Flyway needs separate schema-history and migration connections on PostgreSQL.
            registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
            registry.add("spring.datasource.url", () -> postgres);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.datasource.username", () -> System.getenv("BGT_TEST_POSTGRES_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("BGT_TEST_POSTGRES_PASSWORD"));
            registry.add("spring.datasource.hikari.connection-init-sql", () -> "SELECT 1");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.flyway.placeholders.uuidType", () -> "UUID");
        }
    }

    @Test
    void validatesRatingsWithoutRoundingAndPreservesNulls() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Ratings\"}");
        String play = create(game + "/plays", "{}");
        String review = create(play + "/reviews", "{}");
        try {
            for (String field : List.of("metaRating", "userRating", "playthroughRating", "rating")) {
                String resource = field.equals("rating") ? review : field.equals("playthroughRating") ? play : game;
                for (String value : List.of("0", "9.9", "10", "null", "-0.1", "10.1", "5.55")) {
                    ObjectNode body = mapper.createObjectNode().put("gameTitle", "Ratings");
                    body.set(field, mapper.readTree(value));
                    boolean valid = List.of("0", "9.9", "10", "null").contains(value);
                    for (String method : List.of("POST", "PUT")) {
                        String path = method.equals("POST") ? collection(resource) : resource;
                        var result = mvc.perform(request(HttpMethod.valueOf(method), path)
                                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                                .andExpect(status().is(valid ? (method.equals("POST") ? 201 : 200) : 400));
                        if (valid) {
                            String saved = method.equals("POST") ? result.andReturn().getResponse().getHeader("Location") : resource;
                            var loaded = mapper.readTree(mvc.perform(get(saved)).andExpect(status().isOk())
                                    .andReturn().getResponse().getContentAsString()).get(field);
                            if (value.equals("null")) {
                                assertThat(loaded.isNull()).isTrue();
                            } else {
                                assertThat(loaded.isNumber()).isTrue();
                                assertThat(loaded.decimalValue()).isEqualByComparingTo(value);
                            }
                            if (method.equals("POST")) mvc.perform(delete(saved)).andExpect(status().isNoContent());
                        } else {
                            result.andExpect(jsonPath("$.errors." + field).exists());
                        }
                    }
                }
            }
        } finally {
            mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void validatesOptionalDatesOnCreateAndUpdate() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Dates\"}");
        String play = create(game + "/plays", "{}");
        String review = create(play + "/reviews", "{}");
        try {
            for (String resource : List.of(play, review)) {
                String field = resource.equals(play) ? "completionDate" : "reviewDate";
                for (String value : List.of("\"2024-02-29\"", "null", "\"2025-02-29\"", "\"2026\"",
                        "\"2026-09-09T12:00:00\"", "\"yesterday\"")) {
                    boolean valid = value.equals("\"2024-02-29\"") || value.equals("null");
                    String body = mapper.createObjectNode().set(field, mapper.readTree(value)).toString();
                    for (String method : List.of("POST", "PUT")) {
                        var result = mvc.perform(request(HttpMethod.valueOf(method),
                                        method.equals("POST") ? collection(resource) : resource)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().is(valid ? (method.equals("POST") ? 201 : 200) : 400));
                        if (valid) {
                            String saved = method.equals("POST") ? result.andReturn().getResponse().getHeader("Location") : resource;
                            assertThat(mapper.readTree(mvc.perform(get(saved)).andReturn().getResponse().getContentAsString()).get(field))
                                    .isEqualTo(mapper.readTree(value));
                            if (method.equals("POST")) mvc.perform(delete(saved)).andExpect(status().isNoContent());
                        }
                    }
                }
            }
        } finally {
            mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void keepsManualTimeIndependentAndRecalculatesOnlyTheOwningPlay() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Totals\"}");
        String otherGame = create("/api/v1/games", "{\"gameTitle\":\"Other\"}");
        String first = create(game + "/plays", "{\"timeToBeatMinutes\":300,\"calculatedTimeMinutes\":999}");
        String second = create(game + "/plays", "{\"timeToBeatMinutes\":120}");
        try {
            mvc.perform(get(first)).andExpect(jsonPath("$.calculatedTimeMinutes").value(0));
            String entry = create(first + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":120}");
            String deleted = create(first + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":120}");
            create(second + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":60}");
            mvc.perform(get(first)).andExpect(jsonPath("$.timeToBeatMinutes").value(300))
                    .andExpect(jsonPath("$.calculatedTimeMinutes").value(240));
            mvc.perform(get(second)).andExpect(jsonPath("$.timeToBeatMinutes").value(120))
                    .andExpect(jsonPath("$.calculatedTimeMinutes").value(60));
            var listed = mapper.readTree(mvc.perform(get(game + "/plays")).andReturn().getResponse().getContentAsString());
            assertThat(listed).hasSize(2);
            for (var item : listed) {
                assertThat(item.get("calculatedTimeMinutes").longValue())
                        .isEqualTo(item.get("id").asText().equals(id(first)) ? 240 : 60);
            }
            mvc.perform(put(first).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"timeToBeatMinutes\":300,\"calculatedTimeMinutes\":1}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.calculatedTimeMinutes").value(240));
            mvc.perform(put(entry).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"date\":\"2026-09-09\",\"durationMinutes\":30}")).andExpect(status().isOk());
            mvc.perform(get(first)).andExpect(jsonPath("$.calculatedTimeMinutes").value(150));
            mvc.perform(delete(deleted)).andExpect(status().isNoContent());
            mvc.perform(get(first)).andExpect(jsonPath("$.timeToBeatMinutes").value(300))
                    .andExpect(jsonPath("$.calculatedTimeMinutes").value(30));
            mvc.perform(get(second)).andExpect(jsonPath("$.calculatedTimeMinutes").value(60));
            mvc.perform(get(otherGame + "/plays/" + id(first))).andExpect(status().isNotFound());

            mvc.perform(delete(entry)).andExpect(status().isNoContent());
            mvc.perform(get(first)).andExpect(jsonPath("$.calculatedTimeMinutes").value(0));
            create(first + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":2147483647}");
            create(first + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":2147483647}");
            mvc.perform(get(first)).andExpect(jsonPath("$.calculatedTimeMinutes").value(4294967294L));
            for (String value : List.of("-1", "1.5", "2147483648")) {
                for (String method : List.of("POST", "PUT")) {
                    mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? game + "/plays" : first)
                                    .contentType(MediaType.APPLICATION_JSON).content(mapper.createObjectNode().set("timeToBeatMinutes", mapper.readTree(value)).toString()))
                            .andExpect(status().isBadRequest());
                }
            }
            for (String body : List.of("{\"timeToBeatMinutes\":0}", "{\"timeToBeatMinutes\":null}", "{}")) {
                mvc.perform(put(first).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                        .andExpect(jsonPath("$.calculatedTimeMinutes").value(4294967294L));
            }
        } finally {
            mvc.perform(delete(game)).andExpect(status().isNoContent());
            mvc.perform(delete(otherGame)).andExpect(status().isNoContent());
        }
    }

    @Test
    void ordersCollectionsByTitleOrDateWithNullsLastAndIdTies() throws Exception {
        List<String> games = new ArrayList<>();
        try {
            games.add(create("/api/v1/games", "{\"gameTitle\":\"zeta\"}"));
            games.add(create("/api/v1/games", "{\"gameTitle\":\"Alpha\"}"));
            games.add(create("/api/v1/games", "{\"gameTitle\":\"alpha\"}"));
            List<String> expectedGames = new ArrayList<>(games.subList(1, 3));
            expectedGames.sort(Comparator.comparing(TypedMetadataIntegrationTests::id));
            expectedGames.add(games.getFirst());
            assertOrder("/api/v1/games", expectedGames);

            String game = games.getFirst();
            String noDate = create(game + "/plays", "{}");
            String older = create(game + "/plays", "{\"completionDate\":\"2026-01-01\"}");
            String newer = create(game + "/plays", "{\"completionDate\":\"2026-09-09\"}");
            String tied = create(game + "/plays", "{\"completionDate\":\"2026-09-09\"}");
            List<String> expected = new ArrayList<>(List.of(newer, tied));
            expected.sort(Comparator.comparing(TypedMetadataIntegrationTests::id));
            expected.addAll(List.of(older, noDate));
            assertOrder(game + "/plays", expected);

            String reviews = noDate + "/reviews";
            String undated = create(reviews, "{}");
            String oldReview = create(reviews, "{\"reviewDate\":\"2026-01-01\"}");
            String newReview = create(reviews, "{\"reviewDate\":\"2026-09-09\"}");
            String tiedReview = create(reviews, "{\"reviewDate\":\"2026-09-09\"}");
            List<String> expectedReviews = new ArrayList<>(List.of(newReview, tiedReview));
            expectedReviews.sort(Comparator.comparing(TypedMetadataIntegrationTests::id));
            expectedReviews.addAll(List.of(oldReview, undated));
            assertOrder(reviews, expectedReviews);

            String times = noDate + "/time-entries";
            String oldTime = create(times, "{\"date\":\"2026-01-01\",\"durationMinutes\":1}");
            String newTime = create(times, "{\"date\":\"2026-09-09\",\"durationMinutes\":1}");
            String tiedTime = create(times, "{\"date\":\"2026-09-09\",\"durationMinutes\":1}");
            List<String> expectedTimes = new ArrayList<>(List.of(newTime, tiedTime));
            expectedTimes.sort(Comparator.comparing(TypedMetadataIntegrationTests::id));
            expectedTimes.add(oldTime);
            assertOrder(times, expectedTimes);
        } finally {
            for (String game : games) mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void listQueryCountDoesNotGrowWithPlaythroughCount() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Query count\"}");
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            long firstCount = 0;
            for (int size : List.of(1, 8)) {
                for (int i = size == 1 ? 0 : 1; i < size; i++) {
                    String play = create(game + "/plays", "{}");
                    create(play + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":30}");
                }
                statistics.clear();
                var summaries = plays.list(UUID.fromString(id(game)));
                assertThat(summaries).hasSize(size);
                assertThat(summaries).allSatisfy(summary -> assertThat(summary.calculatedTimeMinutes()).isEqualTo(30));
                if (size == 1) firstCount = statistics.getPrepareStatementCount();
                assertThat(statistics.getPrepareStatementCount()).isEqualTo(firstCount).isLessThanOrEqualTo(2);
                assertThat(statistics.getCollectionFetchCount()).isZero();
            }
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
            mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void rejectsIncorrectJsonTypesWithoutChangingStoredValues() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Strict types\",\"physicalCopy\":true,\"metaRating\":9.5}");
        String play = create(game + "/plays", "{\"coop\":false,\"timeToBeatMinutes\":30}");
        String review = create(play + "/reviews", "{\"reviewTitle\":\"Review\",\"rating\":8}");
        String time = create(play + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":30,\"notes\":\"Session\"}");
        try {
            var invalidValues = Map.of(
                    game, Map.of("gameTitle", List.of("42", "1.5", "true"),
                            "physicalCopy", List.of("2", "\"false\"", "\"\""),
                            "metaRating", List.of("\"9.5\"", "\"\"")),
                    play, Map.of("coop", List.of("0", "\"true\""),
                            "timeToBeatMinutes", List.of("\"30\"", "\"\"")),
                    review, Map.of("reviewTitle", List.of("42"), "rating", List.of("\"8\"")),
                    time, Map.of("durationMinutes", List.of("\"30\""), "notes", List.of("false")));
            for (var resource : invalidValues.entrySet()) {
                String readPath = resource.getKey().equals(time) ? collection(time) : resource.getKey();
                String original = mvc.perform(get(readPath)).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                for (var field : resource.getValue().entrySet()) {
                    for (String value : field.getValue()) {
                        ObjectNode body = (ObjectNode) (resource.getKey().equals(time)
                                ? mapper.readTree(original).get(0) : mapper.readTree(original));
                        body.set(field.getKey(), mapper.readTree(value));
                        for (String method : List.of("POST", "PUT")) {
                            mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST")
                                            ? collection(resource.getKey()) : resource.getKey())
                                            .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                        }
                    }
                }
                mvc.perform(get(readPath)).andExpect(status().isOk()).andExpect(content().json(original));
            }
        } finally {
            mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    @Test
    void childWritesDoNotLoadSiblingRecords() throws Exception {
        String game = create("/api/v1/games", "{\"gameTitle\":\"Child write queries\"}");
        String play = create(game + "/plays", "{}");
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            var collections = Map.of(game + "/plays", "{}", play + "/reviews", "{}",
                    play + "/time-entries", "{\"date\":\"2026-09-09\",\"durationMinutes\":30}");
            for (var collection : collections.entrySet()) {
                for (int i = 0; i < 8; i++) {
                    create(collection.getKey(), collection.getValue());
                }
                statistics.clear();
                String child = create(collection.getKey(), collection.getValue());
                assertThat(statistics.getEntityLoadCount()).isLessThanOrEqualTo(2);
                assertThat(statistics.getCollectionFetchCount()).isZero();

                statistics.clear();
                mvc.perform(delete(child)).andExpect(status().isNoContent());
                assertThat(statistics.getEntityLoadCount()).isLessThanOrEqualTo(3);
                mvc.perform(get(collection.getKey())).andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(collection.getKey().equals(game + "/plays") ? 9 : 8));
            }
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
            mvc.perform(delete(game)).andExpect(status().isNoContent());
        }
    }

    private String create(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
    }

    private void assertOrder(String path, List<String> resources) throws Exception {
        List<String> expected = resources.stream().map(TypedMetadataIntegrationTests::id).toList();
        List<String> actual = new ArrayList<>();
        mapper.readTree(mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .forEach(item -> { if (expected.contains(item.get("id").asText())) actual.add(item.get("id").asText()); });
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private static String collection(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private static String id(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static Path temporaryDirectory() {
        try {
            return Files.createTempDirectory("bgt-typed-test-");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
