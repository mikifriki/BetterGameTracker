package com.bettergametracker.cover;

public class CoverNotFoundException extends RuntimeException {
    public CoverNotFoundException() {
        super("Cover not found");
    }
}
