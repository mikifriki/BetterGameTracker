package com.bettergametracker.review;

public record ReviewRequest(String reviewDate, String reviewTitle, String review, String rating) {
}
