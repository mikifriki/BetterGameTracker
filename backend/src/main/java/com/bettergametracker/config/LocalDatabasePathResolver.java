package com.bettergametracker.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class LocalDatabasePathResolver {

    private static final String DATABASE_FILE_NAME = "better-game-tracker.db";

    public Path resolve(Map<String, String> environment, String operatingSystem, Path userHome) throws IOException {
        String configuredPath = environment.get("BETTER_GAME_TRACKER_DATABASE_PATH");
        Path databasePath = configuredPath == null || configuredPath.isBlank()
                ? defaultPath(environment, operatingSystem, userHome)
                : Path.of(configuredPath);

        Path absoluteDatabasePath = databasePath.toAbsolutePath().normalize();
        Files.createDirectories(absoluteDatabasePath.getParent());
        return absoluteDatabasePath;
    }

    private Path defaultPath(Map<String, String> environment, String operatingSystem, Path userHome) {
        if (operatingSystem.startsWith("Windows")) {
            String localAppData = environment.get("LOCALAPPDATA");
            Path baseDirectory = localAppData == null || localAppData.isBlank()
                    ? userHome.resolve("AppData/Local")
                    : Path.of(localAppData);
            return baseDirectory.resolve("BetterGameTracker").resolve(DATABASE_FILE_NAME);
        }
        if (operatingSystem.startsWith("Mac")) {
            return userHome.resolve("Library/Application Support/BetterGameTracker").resolve(DATABASE_FILE_NAME);
        }

        String xdgDataHome = environment.get("XDG_DATA_HOME");
        Path baseDirectory = xdgDataHome == null || xdgDataHome.isBlank()
                ? userHome.resolve(".local/share")
                : Path.of(xdgDataHome);
        return baseDirectory.resolve("BetterGameTracker").resolve(DATABASE_FILE_NAME);
    }
}
