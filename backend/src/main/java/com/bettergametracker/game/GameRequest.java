package com.bettergametracker.game;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameRequest(
        @NotBlank @Size(max = 255) String gameTitle,
        @Size(max = 1000) String description,
        @Size(max = 255) String releasePlatform,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
        LocalDate releaseDate,
        @Size(max = 255) String developer,
        @DecimalMin("0") @DecimalMax("10") @Digits(integer = 2, fraction = 1) BigDecimal metaRating,
        @DecimalMin("0") @DecimalMax("10") @Digits(integer = 2, fraction = 1) BigDecimal userRating,
        Boolean physicalCopy) {
}
