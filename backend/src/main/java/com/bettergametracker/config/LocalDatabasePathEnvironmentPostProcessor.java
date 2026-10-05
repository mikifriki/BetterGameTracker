package com.bettergametracker.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

public final class LocalDatabasePathEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_NAME = "better-game-tracker.local.database-path";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean local = environment.acceptsProfiles(Profiles.of("local"));
        boolean hosted = environment.acceptsProfiles(Profiles.of("hosted"));
        if (local == hosted) {
            throw new IllegalStateException("Select exactly one deployment profile: local or hosted");
        }
        if (!local) {
            return;
        }

        try {
            String configuredPath = environment.getProperty("BETTER_GAME_TRACKER_DATABASE_PATH");
            Path databasePath = new LocalDatabasePathResolver().resolve(configuredPath);
            Path coverDirectory = configuredPath == null || configuredPath.isBlank()
                    ? databasePath.getParent().resolve("covers") : Path.of(databasePath + ".covers");
            environment.getPropertySources().addFirst(new MapPropertySource("localDatabasePath", Map.of(
                    PROPERTY_NAME, databasePath.toString(),
                    "better-game-tracker.local.cover-directory", coverDirectory.toString())));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize local storage: " + exception.getMessage(), exception);
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
