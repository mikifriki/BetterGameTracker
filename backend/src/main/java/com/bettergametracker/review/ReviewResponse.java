package com.bettergametracker.review;

import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID playId,
        String reviewDate,
        String reviewTitle,
        String review,
        String rating) {
}
