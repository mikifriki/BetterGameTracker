package com.bettergametracker.review;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
        LocalDate reviewDate,
        @Size(max = 255) String reviewTitle,
        @Size(max = 5000) String review,
        @DecimalMin("0") @DecimalMax("10") @Digits(integer = 2, fraction = 1) BigDecimal rating) {
}
