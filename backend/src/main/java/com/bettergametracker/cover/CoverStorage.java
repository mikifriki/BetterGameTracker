package com.bettergametracker.cover;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class CoverStorage {
    private final Path directory;

    public CoverStorage(@Value("${better-game-tracker.cover-directory}") Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    public byte[] read(UUID gameId) throws IOException {
        try {
            return Files.readAllBytes(directory.resolve(gameId + ".image"));
        } catch (NoSuchFileException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cover not found");
        }
    }

    public void write(UUID gameId, byte[] bytes) throws IOException {
        if (bytes.length > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Cover must be at most 5 MB");
        }
        contentType(bytes);
        Path temporary = Files.createTempFile(directory, ".upload-", ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, directory.resolve(gameId + ".image"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void delete(UUID gameId) throws IOException {
        Files.deleteIfExists(directory.resolve(gameId + ".image"));
    }

    public static String contentType(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                && bytes[3] == 'G' && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            return "image/png";
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) {
            return "image/jpeg";
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Upload a PNG or JPEG image");
    }
}
