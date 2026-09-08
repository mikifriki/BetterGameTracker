package com.bettergametracker.game;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.context.annotation.Import(com.bettergametracker.security.SecurityConfiguration.class)
@org.springframework.test.context.ActiveProfiles("local")
@WebMvcTest(GameController.class)
class GameControllerTests {

    private static final UUID GAME_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void listsGamesAsDtosWithoutRelationships() throws Exception {
        when(gameService.list()).thenReturn(List.of(game(GAME_ID, "Example")));

        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(GAME_ID.toString()))
                .andExpect(jsonPath("$[0].gameTitle").value("Example"))
                .andExpect(jsonPath("$[0].playEntries").doesNotExist());
    }

    @Test
    void createsGameAndReturnsLocation() throws Exception {
        when(gameService.create(any(Game.class))).thenReturn(game(GAME_ID, "Example"));

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Example")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/games/" + GAME_ID))
                .andExpect(jsonPath("$.id").value(GAME_ID.toString()))
                .andExpect(jsonPath("$.gameTitle").value("Example"));
    }

    @Test
    void getsAndUpdatesGame() throws Exception {
        when(gameService.get(GAME_ID)).thenReturn(game(GAME_ID, "Original"));
        when(gameService.update(any(UUID.class), any(Game.class))).thenReturn(game(GAME_ID, "Updated"));

        mockMvc.perform(get("/api/v1/games/{gameId}", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameTitle").value("Original"));

        mockMvc.perform(put("/api/v1/games/{gameId}", GAME_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GAME_ID.toString()))
                .andExpect(jsonPath("$.gameTitle").value("Updated"));
    }

    @Test
    void deletesGameWithoutAResponseBody() throws Exception {
        mockMvc.perform(delete("/api/v1/games/{gameId}", GAME_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(gameService).delete(GAME_ID);
    }

    @Test
    void reportsUnknownGamesAsProblemDetails() throws Exception {
        when(gameService.get(GAME_ID)).thenThrow(new GameNotFoundException(GAME_ID));

        mockMvc.perform(get("/api/v1/games/{gameId}", GAME_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Game not found: " + GAME_ID));
    }

    @Test
    void rejectsMissingAndBlankRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameTitle\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.gameTitle").exists())
                .andExpect(jsonPath("$.errors.description").doesNotExist());
    }

    @Test
    void rejectsMalformedUuidAndJson() throws Exception {
        mockMvc.perform(get("/api/v1/games/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid value for gameId"));

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request body is not valid JSON"));
    }

    @Test
    void preservesFrameworkClientErrors() throws Exception {
        mockMvc.perform(post("/api/v1/games/{gameId}", GAME_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid content type"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void hidesUnexpectedServerErrors() throws Exception {
        when(gameService.list()).thenThrow(new RuntimeException("Sensitive internal detail"));

        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("Sensitive internal detail"))));
    }

    private static Game game(UUID id, String title) {
        Game game = new Game(title);
        ReflectionTestUtils.setField(game, "id", id);
        game.setDescription("Description");
        game.setReleasePlatform("PC");
        game.setReleaseDate("2026-09-07");
        game.setDeveloper("Developer");
        game.setMetaRating("90");
        game.setUserRating("9.0");
        game.setPhysicalCopy("Yes");
        return game;
    }

    private static String validRequest(String title) {
        return """
                {"gameTitle":"%s","description":"Description","releasePlatform":"PC",
                 "releaseDate":"2026-09-07","developer":"Developer","metaRating":"90",
                 "userRating":"9.0","physicalCopy":"Yes"}
                """.formatted(title);
    }
}
