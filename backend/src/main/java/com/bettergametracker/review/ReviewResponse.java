package com.bettergametracker.review;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID playId,
        LocalDate reviewDate,
        String reviewTitle,
        String review,
        BigDecimal rating) {
}
