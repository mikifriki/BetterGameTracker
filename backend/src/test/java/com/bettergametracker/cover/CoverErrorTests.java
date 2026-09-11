package com.bettergametracker.cover;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import com.bettergametracker.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoverErrorTests {
    @TempDir Path directory;

    @Test
    void rejectsNullImageBytes() throws IOException {
        CoverStorage storage = new CoverStorage(directory);
        assertThatThrownBy(() -> storage.write(UUID.randomUUID(), null))
                .isInstanceOf(UnsupportedCoverException.class);
        assertThatThrownBy(() -> CoverStorage.contentType(null))
                .isInstanceOf(UnsupportedCoverException.class);
    }

    @Test
    void filesystemFailureReturnsGenericProblem() throws Exception {
        CoverService covers = mock(CoverService.class);
        UUID gameId = UUID.randomUUID();
        when(covers.get(gameId)).thenThrow(new IOException("Cannot read /private/covers/game.image"));
        String path = "/api/v1/games/" + gameId + "/cover";

        MockMvcBuilders.standaloneSetup(new CoverController(covers))
                .setControllerAdvice(new ApiExceptionHandler()).build()
                .perform(get(path))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value(path));
    }
}
