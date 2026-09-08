package com.bettergametracker.game;

import jakarta.validation.constraints.NotBlank;

public record GameRequest(
        @NotBlank String gameTitle,
        String description,
        String releasePlatform,
        String releaseDate,
        String developer,
        String metaRating,
        String userRating,
        String physicalCopy) {
}
