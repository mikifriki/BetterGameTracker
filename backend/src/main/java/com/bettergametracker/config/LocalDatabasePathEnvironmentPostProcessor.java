package com.bettergametracker.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
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
        if (!environment.acceptsProfiles(Profiles.of("local"))) {
            return;
        }

        try {
            Map<String, String> processEnvironment = new HashMap<>(System.getenv());
            String configuredPath = environment.getProperty("BETTER_GAME_TRACKER_DATABASE_PATH");
            if (configuredPath != null) {
                processEnvironment.put("BETTER_GAME_TRACKER_DATABASE_PATH", configuredPath);
            }
            Path databasePath = new LocalDatabasePathResolver().resolve(
                    processEnvironment, System.getProperty("os.name"), Path.of(System.getProperty("user.home")));
            environment.getPropertySources().addFirst(
                    new MapPropertySource("localDatabasePath", Map.of(PROPERTY_NAME, databasePath.toString())));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the local BetterGameTracker data directory", exception);
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
