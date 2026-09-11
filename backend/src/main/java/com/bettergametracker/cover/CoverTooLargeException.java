package com.bettergametracker.cover;

public class CoverTooLargeException extends RuntimeException {
    public CoverTooLargeException() {
        super("Cover must be at most 5 MB");
    }
}
