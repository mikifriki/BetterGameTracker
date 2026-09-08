package com.bettergametracker.play;

import java.util.UUID;

public record PlayEntryResponse(
        UUID id,
        UUID gameId,
        String playthroughRating,
        String completionDate,
        String platformPlayedOn,
        String timeToBeat,
        String completionRate,
        String coop,
        String location) {
}
