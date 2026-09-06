package com.bettergametracker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.bettergametracker.config.LocalDatabasePathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationLoadingTests {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void commonConfigurationEnablesFlywayAndValidatesHibernateMappings() throws IOException {
        Map<String, Object> properties = propertiesFrom("application.yml");

        assertThat(propertyValue(properties, "spring.flyway.enabled")).isEqualTo("true");
        assertThat(propertyValue(properties, "spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(propertyValue(properties, "spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void localConfigurationUsesPersistentSQLiteAndLoopbackBinding() throws IOException {
        Map<String, Object> properties = propertiesFrom("application-local.yml");

        assertThat(propertyValue(properties, "spring.datasource.driver-class-name")).isEqualTo("org.sqlite.JDBC");
        assertThat(propertyValue(properties, "spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
        assertThat(propertyValue(properties, "server.address")).isEqualTo("127.0.0.1");
        assertThat(propertyValue(properties, "spring.datasource.url"))
                .isEqualTo("jdbc:sqlite:${better-game-tracker.local.database-path}");
    }

    @Test
    void hostedConfigurationUsesPostgreSqlEnvironmentConnectionSettings() throws IOException {
        Map<String, Object> properties = propertiesFrom("application-hosted.yml");

        assertThat(propertyValue(properties, "spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(propertyValue(properties, "spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
        assertThat(propertyValue(properties, "spring.datasource.url"))
                .isEqualTo("${BETTER_GAME_TRACKER_DATABASE_URL}");
        assertThat(propertyValue(properties, "spring.datasource.username"))
                .isEqualTo("${BETTER_GAME_TRACKER_DATABASE_USERNAME}");
        assertThat(propertyValue(properties, "spring.datasource.password"))
                .isEqualTo("${BETTER_GAME_TRACKER_DATABASE_PASSWORD}");
    }

    @Test
    void hostedProfileLoadsPostgreSqlConnectionValuesFromTheEnvironment() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(
                        "spring.profiles.active=hosted",
                        "BETTER_GAME_TRACKER_DATABASE_URL=jdbc:postgresql://localhost:5432/better_game_tracker",
                        "BETTER_GAME_TRACKER_DATABASE_USERNAME=test-user",
                        "BETTER_GAME_TRACKER_DATABASE_PASSWORD=test-password")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://localhost:5432/better_game_tracker");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("test-user");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                            .isEqualTo("test-password");
                    assertThat(context.getEnvironment().getProperty("spring.jpa.database-platform"))
                            .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
                });
    }

    @Test
    void localDatabaseResolverUsesMacApplicationSupportAndCreatesItsDirectory(@TempDir Path temporaryDirectory)
            throws IOException {
        Path userHome = temporaryDirectory.resolve("user-home");

        Path databasePath = new LocalDatabasePathResolver().resolve(Map.of(), "Mac OS X", userHome);

        assertThat(databasePath)
                .isEqualTo(userHome.resolve("Library/Application Support/BetterGameTracker/better-game-tracker.db"));
        assertThat(databasePath.getParent()).isDirectory();
    }

    @Test
    void localDatabaseResolverHonorsConfiguredDatabasePath(@TempDir Path temporaryDirectory) throws IOException {
        Path configuredPath = temporaryDirectory.resolve("custom/data.db");

        Path databasePath = new LocalDatabasePathResolver().resolve(
                Map.of("BETTER_GAME_TRACKER_DATABASE_PATH", configuredPath.toString()), "Linux", temporaryDirectory);

        assertThat(databasePath).isEqualTo(configuredPath);
        assertThat(databasePath.getParent()).isDirectory();
    }

    private Map<String, Object> propertiesFrom(String resourceName) throws IOException {
        List<PropertySource<?>> propertySources = loader.load(resourceName, new ClassPathResource(resourceName));
        Map<String, Object> properties = new java.util.HashMap<>();
        propertySources.forEach(source -> flatten("", source.getSource(), properties));
        return properties;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Object value, Map<String, Object> flattenedProperties) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, nestedValue) -> flatten(
                    prefix.isEmpty() ? key.toString() : prefix + "." + key, nestedValue, flattenedProperties));
            return;
        }
        flattenedProperties.put(prefix, value);
    }

    private String propertyValue(Map<String, Object> properties, String propertyName) {
        return properties.get(propertyName).toString();
    }
}
