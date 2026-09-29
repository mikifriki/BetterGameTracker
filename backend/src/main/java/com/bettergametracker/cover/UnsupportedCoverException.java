package com.bettergametracker.cover;

public class UnsupportedCoverException extends RuntimeException {
    public UnsupportedCoverException() {
        super("Upload a PNG or JPEG image");
    }
}
