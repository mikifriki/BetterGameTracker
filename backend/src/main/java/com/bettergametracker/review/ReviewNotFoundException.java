package com.bettergametracker.review;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(UUID playEntryId, UUID reviewId) {
        super("Review not found for play entry " + playEntryId + ": " + reviewId);
    }
}
