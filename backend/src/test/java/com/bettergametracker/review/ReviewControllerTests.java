package com.bettergametracker.review;

import java.util.List;
import java.util.UUID;

import com.bettergametracker.game.GameNotFoundException;
import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.play.PlayEntryNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.context.annotation.Import(com.bettergametracker.security.SecurityConfiguration.class)
@org.springframework.test.context.ActiveProfiles("local")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ReviewController.class)
class ReviewControllerTests {

    private static final UUID GAME_ID = UUID.randomUUID();
    private static final UUID PLAY_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final String BASE_PATH = "/api/v1/games/" + GAME_ID + "/plays/" + PLAY_ID + "/reviews";
    private static final String REQUEST = """
            {"reviewDate":"2026-09-08","reviewTitle":"Great game","review":"Enjoyed it","rating":9}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void listsAndGetsFlatDtosWithAllFields() throws Exception {
        when(reviewService.list(GAME_ID, PLAY_ID)).thenReturn(List.of(review()));
        when(reviewService.get(GAME_ID, PLAY_ID, REVIEW_ID)).thenReturn(review());
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REVIEW_ID.toString()))
                .andExpect(jsonPath("$[0].playId").value(PLAY_ID.toString()));
        mockMvc.perform(get(BASE_PATH + "/" + REVIEW_ID)).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","playId":"%s","reviewDate":"2026-09-08",
                         "reviewTitle":"Great game","review":"Enjoyed it","rating":9}
                        """.formatted(REVIEW_ID, PLAY_ID), JsonCompareMode.STRICT));
    }

    @Test
    void listsAnEmptyCollection() throws Exception {
        when(reviewService.list(GAME_ID, PLAY_ID)).thenReturn(List.of());
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void mapsFieldsAndUsesOnlyPathOwnership(String method) throws Exception {
        when(reviewService.create(eq(GAME_ID), eq(PLAY_ID), any(Review.class))).thenAnswer(invocation -> {
            assertRequest(invocation.getArgument(2));
            return review();
        });
        when(reviewService.update(eq(GAME_ID), eq(PLAY_ID), eq(REVIEW_ID), any(Review.class)))
                .thenAnswer(invocation -> {
                    assertRequest(invocation.getArgument(3));
                    return review();
                });
        var result = mockMvc.perform(request(HttpMethod.valueOf(method),
                        method.equals("POST") ? BASE_PATH : BASE_PATH + "/" + REVIEW_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("{", "{\"id\":\"" + UUID.randomUUID()
                        + "\",\"playId\":\"" + UUID.randomUUID() + "\",\"gameId\":\""
                        + UUID.randomUUID() + "\",")))
                .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()))
                .andExpect(jsonPath("$.playId").value(PLAY_ID.toString()));
        if (method.equals("POST")) {
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", BASE_PATH + "/" + REVIEW_ID));
        } else {
            result.andExpect(status().isOk());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void preservesMissingNullAndBlankOptionalFields(String method) throws Exception {
        when(reviewService.create(eq(GAME_ID), eq(PLAY_ID), any(Review.class))).thenAnswer(invocation ->
                attach(invocation.getArgument(2)));
        when(reviewService.update(eq(GAME_ID), eq(PLAY_ID), eq(REVIEW_ID), any(Review.class)))
                .thenAnswer(invocation -> attach(invocation.getArgument(3)));
        for (String body : List.of("{}", """
                {"reviewDate":null,"reviewTitle":null,"review":null,"rating":null}
                """, """
                {"reviewDate":null,"reviewTitle":" ","review":" ","rating":null}
                """)) {
            String value = body.contains(":\" \"") ? "\" \"" : "null";
            mockMvc.perform(request(HttpMethod.valueOf(method),
                            method.equals("POST") ? BASE_PATH : BASE_PATH + "/" + REVIEW_ID)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().is(method.equals("POST") ? 201 : 200))
                    .andExpect(content().json("""
                            {"id":"%s","playId":"%s","reviewDate":%s,"reviewTitle":%s,"review":%s,"rating":%s}
                            """.formatted(REVIEW_ID, PLAY_ID, "null", value, value, "null"), JsonCompareMode.STRICT));
        }
    }

    @Test
    void deletesWithoutAResponseBody() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/" + REVIEW_ID))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(reviewService).delete(GAME_ID, PLAY_ID, REVIEW_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIST", "POST", "GET", "PUT", "DELETE"})
    void reportsMissingOrWrongParentsAsNotFound(String operation) throws Exception {
        for (RuntimeException exception : List.of(new GameNotFoundException(GAME_ID),
                new PlayEntryNotFoundException(GAME_ID, PLAY_ID))) {
            assertNotFound(operation, exception);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void reportsMissingOrWrongOwnerReviewsAsNotFound(String operation) throws Exception {
        assertNotFound(operation, new ReviewNotFoundException(PLAY_ID, REVIEW_ID));
    }

    @Test
    void rejectsMalformedIdsAndBodiesWithoutCallingService() throws Exception {
        for (String path : List.of(BASE_PATH.replace(GAME_ID.toString(), "invalid"),
                BASE_PATH.replace(PLAY_ID.toString(), "invalid"), BASE_PATH + "/invalid")) {
            mockMvc.perform(get(path)).andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
        for (String method : List.of("POST", "PUT")) {
            for (String body : List.of("{", "null", "", "[]")) {
                mockMvc.perform(request(HttpMethod.valueOf(method),
                                method.equals("POST") ? BASE_PATH : BASE_PATH + "/" + REVIEW_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
            }
        }
        verifyNoInteractions(reviewService);
    }

    private void assertNotFound(String operation, RuntimeException exception) throws Exception {
        doThrow(exception).when(reviewService).list(GAME_ID, PLAY_ID);
        doThrow(exception).when(reviewService).create(eq(GAME_ID), eq(PLAY_ID), any(Review.class));
        doThrow(exception).when(reviewService).get(GAME_ID, PLAY_ID, REVIEW_ID);
        doThrow(exception).when(reviewService).update(eq(GAME_ID), eq(PLAY_ID), eq(REVIEW_ID), any(Review.class));
        doThrow(exception).when(reviewService).delete(GAME_ID, PLAY_ID, REVIEW_ID);
        mockMvc.perform(request(HttpMethod.valueOf(operation.equals("LIST") ? "GET" : operation),
                        operation.equals("LIST") || operation.equals("POST") ? BASE_PATH : BASE_PATH + "/" + REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(exception.getMessage()));
    }

    private static void assertRequest(Review review) {
        assertThat(review.getId()).isNull();
        assertThat(review.getPlayEntry()).isNull();
        assertThat(review.getReviewDate()).isEqualTo(java.time.LocalDate.parse("2026-09-08"));
        assertThat(review.getReviewTitle()).isEqualTo("Great game");
        assertThat(review.getReview()).isEqualTo("Enjoyed it");
        assertThat(review.getRating()).isEqualByComparingTo("9");
    }

    private static Review review() {
        Review review = new Review();
        review.setReviewDate(java.time.LocalDate.parse("2026-09-08"));
        review.setReviewTitle("Great game");
        review.setReview("Enjoyed it");
        review.setRating(new java.math.BigDecimal("9"));
        return attach(review);
    }

    private static Review attach(Review review) {
        PlayEntry play = new PlayEntry();
        ReflectionTestUtils.setField(play, "id", PLAY_ID);
        play.addReview(review);
        ReflectionTestUtils.setField(review, "id", REVIEW_ID);
        return review;
    }
}
