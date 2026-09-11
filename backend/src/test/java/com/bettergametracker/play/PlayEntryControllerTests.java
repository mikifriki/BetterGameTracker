package com.bettergametracker.play;

import java.util.List;
import java.util.UUID;

import com.bettergametracker.game.Game;
import com.bettergametracker.game.GameNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.context.annotation.Import(com.bettergametracker.security.SecurityConfiguration.class)
@org.springframework.test.context.ActiveProfiles("local")
@WebMvcTest(PlayEntryController.class)
class PlayEntryControllerTests {

    private static final UUID GAME_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String BASE_PATH = "/api/v1/games/" + GAME_ID + "/plays";
    private static final String REQUEST = """
            {
              "playthroughRating": 9,
              "completionDate": "2026-09-07",
              "platformPlayedOn": "PC",
              "timeToBeatMinutes": 1200,
              "completionStatus": "COMPLETE",
              "coop": true,
              "location": "Home"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayEntryService playEntryService;

    @Test
    void listsAndGetsDtosWithoutNestedRelationships() throws Exception {
        when(playEntryService.list(GAME_ID)).thenReturn(List.of(new PlayEntrySummary(playEntry(), 0)));
        when(playEntryService.getWithTime(GAME_ID, PLAY_ID)).thenReturn(new PlayEntrySummary(playEntry(), 0));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PLAY_ID.toString()))
                .andExpect(jsonPath("$[0].gameId").value(GAME_ID.toString()));
        mockMvc.perform(get(BASE_PATH + "/" + PLAY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAY_ID.toString()))
                .andExpect(jsonPath("$.gameId").value(GAME_ID.toString()))
                .andExpect(jsonPath("$.playthroughRating").value(9))
                .andExpect(jsonPath("$.completionDate").value("2026-09-07"))
                .andExpect(jsonPath("$.platformPlayedOn").value("PC"))
                .andExpect(jsonPath("$.timeToBeatMinutes").value(1200))
                .andExpect(jsonPath("$.completionStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.coop").value(true))
                .andExpect(jsonPath("$.location").value("Home"))
                .andExpect(jsonPath("$.game").doesNotExist())
                .andExpect(jsonPath("$.reviews").doesNotExist())
                .andExpect(jsonPath("$.timeEntries").doesNotExist());
    }

    @Test
    void listsAnEmptyCollection() throws Exception {
        when(playEntryService.list(GAME_ID)).thenReturn(List.of());
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void mapsAllRequestFieldsAndUsesPathOwnership(String method) throws Exception {
        when(playEntryService.create(eq(GAME_ID), any(PlayEntry.class))).thenAnswer(invocation -> {
            assertRequest(invocation.getArgument(1));
            return new PlayEntrySummary(playEntry(), 0);
        });
        when(playEntryService.update(eq(GAME_ID), eq(PLAY_ID), any(PlayEntry.class))).thenAnswer(invocation -> {
            assertRequest(invocation.getArgument(2));
            return new PlayEntrySummary(playEntry(), 0);
        });

        var result = mockMvc.perform(request(HttpMethod.valueOf(method),
                        method.equals("POST") ? BASE_PATH : BASE_PATH + "/" + PLAY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("{", "{\"gameId\":\"" + UUID.randomUUID() + "\",")))
                .andExpect(jsonPath("$.id").value(PLAY_ID.toString()))
                .andExpect(jsonPath("$.gameId").value(GAME_ID.toString()));
        if (method.equals("POST")) {
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", BASE_PATH + "/" + PLAY_ID));
        } else {
            result.andExpect(status().isOk());
        }
    }

    @Test
    void deletesWithoutAResponseBody() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/" + PLAY_ID))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(playEntryService).delete(GAME_ID, PLAY_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void reportsMissingOrWrongOwnerPlaysAsNotFound(String method) throws Exception {
        PlayEntryNotFoundException exception = new PlayEntryNotFoundException(GAME_ID, PLAY_ID);
        when(playEntryService.getWithTime(GAME_ID, PLAY_ID)).thenThrow(exception);
        when(playEntryService.update(eq(GAME_ID), eq(PLAY_ID), any(PlayEntry.class))).thenThrow(exception);
        doThrow(exception).when(playEntryService).delete(GAME_ID, PLAY_ID);

        mockMvc.perform(request(HttpMethod.valueOf(method), BASE_PATH + "/" + PLAY_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(exception.getMessage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void reportsMissingParentAsNotFound(String method) throws Exception {
        when(playEntryService.list(GAME_ID)).thenThrow(new GameNotFoundException(GAME_ID));
        when(playEntryService.create(eq(GAME_ID), any(PlayEntry.class)))
                .thenThrow(new GameNotFoundException(GAME_ID));
        mockMvc.perform(request(HttpMethod.valueOf(method), BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsMalformedIdsAndJson() throws Exception {
        for (String path : List.of("/api/v1/games/invalid/plays", BASE_PATH + "/invalid")) {
            mockMvc.perform(get(path)).andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(playEntryService);
    }

    private static void assertRequest(PlayEntry entry) {
        assertThat(entry).usingRecursiveComparison()
                .ignoringFields("id", "game", "initialGame").isEqualTo(playEntry());
        assertThat(entry.getId()).isNull();
        assertThat(entry.getGame()).isNull();
    }

    private static PlayEntry playEntry() {
        Game game = new Game("Game");
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        PlayEntry entry = new PlayEntry();
        ReflectionTestUtils.setField(entry, "id", PLAY_ID);
        entry.setPlaythroughRating(new java.math.BigDecimal("9"));
        entry.setCompletionDate(java.time.LocalDate.parse("2026-09-07"));
        entry.setPlatformPlayedOn("PC");
        entry.setTimeToBeatMinutes(1200);
        entry.setCompletionStatus(com.bettergametracker.play.CompletionStatus.COMPLETE);
        entry.setCoop(true);
        entry.setLocation("Home");
        game.addPlayEntry(entry);
        return entry;
    }
}
