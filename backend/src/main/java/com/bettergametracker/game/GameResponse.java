package com.bettergametracker.game;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GameResponse(
        UUID id,
        String gameTitle,
        String description,
        String releasePlatform,
        LocalDate releaseDate,
        String developer,
        BigDecimal metaRating,
        BigDecimal userRating,
        Boolean physicalCopy) {
}
