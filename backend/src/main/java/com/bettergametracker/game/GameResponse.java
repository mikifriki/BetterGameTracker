package com.bettergametracker.game;

import java.util.UUID;

public record GameResponse(
        UUID id,
        String gameTitle,
        String description,
        String releasePlatform,
        String releaseDate,
        String developer,
        String metaRating,
        String userRating,
        String physicalCopy) {
}
