package com.bettergametracker.play;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PlayEntryRequest(
        @DecimalMin("0") @DecimalMax("10") @Digits(integer = 2, fraction = 1) BigDecimal playthroughRating,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
        LocalDate completionDate,
        @Size(max = 255) String platformPlayedOn,
        @PositiveOrZero Integer timeToBeatMinutes,
        CompletionStatus completionStatus,
        Boolean coop,
        @Size(max = 255) String location) {
}
