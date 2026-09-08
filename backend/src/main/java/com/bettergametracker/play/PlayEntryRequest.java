package com.bettergametracker.play;

import jakarta.validation.constraints.NotBlank;

public record PlayEntryRequest(
        @NotBlank String playthroughRating,
        @NotBlank String completionDate,
        @NotBlank String platformPlayedOn,
        String timeToBeat,
        @NotBlank String completionRate,
        String coop,
        @NotBlank String location) {
}
