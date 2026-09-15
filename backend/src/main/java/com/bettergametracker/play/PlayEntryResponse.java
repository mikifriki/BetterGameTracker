package com.bettergametracker.play;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PlayEntryResponse(
        UUID id,
        UUID gameId,
        BigDecimal playthroughRating,
        LocalDate startDate,
        LocalDate completionDate,
        String platformPlayedOn,
        Integer timeToBeatMinutes,
        CompletionStatus completionStatus,
        Boolean coop,
        String location,
        long calculatedTimeMinutes) {
}
