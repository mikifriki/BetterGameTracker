package com.bettergametracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.bettergametracker.config.LocalDatabasePathResolver;
import com.bettergametracker.config.LocalDatabasePathEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void localConfigurationUsesPersistentSQLiteAndLanBinding() throws IOException {
        Map<String, Object> properties = propertiesFrom("application-local.yml");

        assertThat(propertyValue(properties, "spring.datasource.driver-class-name")).isEqualTo("org.sqlite.JDBC");
        assertThat(propertyValue(properties, "spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
        assertThat(propertyValue(properties, "server.address")).isEqualTo("0.0.0.0");
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
    void defaultLayoutHandlesBlankOverridesAndExistingDirectories(@TempDir Path directory) throws IOException {
        Path installation = directory.resolve("Mängud with spaces");
        var resolver = new LocalDatabasePathResolver();
        Path expected = installation.resolve("db/better-game-tracker.db");
        assertThat(resolver.resolve(null, installation)).isEqualTo(expected);
        assertThat(resolver.resolve("  ", installation)).isEqualTo(expected);
        assertThat(expected.getParent()).isDirectory();
    }

    @Test
    void databaseOverrideBypassesLocationDiscoveryAndKeepsRelativePaths(@TempDir Path directory) throws IOException {
        Path configured = directory.resolve("custom/data.db");
        Path relative = Path.of("").toAbsolutePath().relativize(configured);
        assertThat(new LocalDatabasePathResolver().resolve(relative.toString(), null)).isEqualTo(configured);
        assertThat(new LocalDatabasePathResolver().resolve(configured.toString())).isEqualTo(configured);
    }

    @Test
    void developmentLaunchDiscoversLocalStorageUnderJUnit() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        new LocalDatabasePathEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        Path storage = Path.of("").toAbsolutePath().resolve("db");
        assertThat(environment.getProperty("better-game-tracker.local.database-path"))
                .isEqualTo(storage.resolve("better-game-tracker.db").toString());
        assertThat(environment.getProperty("better-game-tracker.local.cover-directory"))
                .isEqualTo(storage.resolve("covers").toString());
    }

    @Test
    void resolvesPackagedAndDevelopmentLocations(@TempDir Path directory) throws IOException {
        var resolver = new LocalDatabasePathResolver();
        Path jar = Files.createFile(directory.resolve("Mängud app.jar"));
        Path executable = Files.createFile(directory.resolve("BetterGameTracker.exe"));
        Path classes = directory.resolve("classes");
        Files.createDirectories(classes.resolve("com/bettergametracker"));
        Files.createFile(classes.resolve("com/bettergametracker/BetterGameTrackerApplication.class"));
        Path launch = directory.resolve("launch");
        assertThat(resolver.applicationDirectory(jar, false, launch)).isEqualTo(directory.toRealPath());
        assertThat(resolver.applicationDirectory(executable, true, launch)).isEqualTo(directory.toRealPath());
        assertThat(resolver.applicationDirectory(classes, false, launch)).isEqualTo(launch);
        assertThatThrownBy(() -> resolver.applicationDirectory(null, false, launch)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> resolver.applicationDirectory(null, true, launch)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> resolver.applicationDirectory(Path.of("relative.exe"), true, launch))
                .isInstanceOf(IOException.class).hasMessageContaining("absolute application location");
        assertThatThrownBy(() -> resolver.applicationDirectory(directory, false, launch))
                .isInstanceOf(IOException.class).hasMessageContaining("Unrecognized application location");
        assertThatThrownBy(() -> resolver.applicationDirectory(executable, false, launch))
                .isInstanceOf(IOException.class);
    }

    @Test
    void obstructedStorageFailsWithItsPath(@TempDir Path directory) throws IOException {
        Path obstruction = Files.createFile(directory.resolve("db"));
        assertThatThrownBy(() -> new LocalDatabasePathResolver().resolve(null, directory))
                .isInstanceOf(IOException.class).hasMessageContaining(obstruction.toString());
        var environment = localEnvironment(directory.resolve("db/app.db"));
        assertThatThrownBy(() -> new LocalDatabasePathEnvironmentPostProcessor().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(obstruction.toString());
    }

    @Test
    void localPropertiesPreserveDatabaseOverrideAndCoverDefault(@TempDir Path directory) throws IOException {
        Path database = directory.resolve("custom/app.db");
        var environment = localEnvironment(database);
        new LocalDatabasePathEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:" + database);
        assertThat(environment.getProperty("better-game-tracker.cover-directory")).isEqualTo(database + ".covers");
    }

    @Test
    void explicitCoverOverridesArePreserved(@TempDir Path directory) throws IOException {
        Path database = directory.resolve("app.db");
        for (String property : List.of("BETTER_GAME_TRACKER_COVER_DIRECTORY", "better-game-tracker.cover-directory")) {
            var environment = localEnvironment(database);
            Path covers = directory.resolve("Mängud covers");
            environment.setProperty(property, covers.toString());
            new LocalDatabasePathEnvironmentPostProcessor().postProcessEnvironment(environment, null);
            assertThat(environment.getProperty("better-game-tracker.cover-directory")).isEqualTo(covers.toString());
        }
    }

    @Test
    void hostedProcessingDoesNotTouchLocalStorage(@TempDir Path directory) {
        var environment = new MockEnvironment().withProperty("BETTER_GAME_TRACKER_DATABASE_PATH",
                directory.resolve("unused/app.db").toString());
        environment.setActiveProfiles("hosted");
        new LocalDatabasePathEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        assertThat(directory.resolve("unused")).doesNotExist();
        assertThat(environment.getProperty("better-game-tracker.local.database-path")).isNull();
    }

    private MockEnvironment localEnvironment(Path database) throws IOException {
        var environment = new MockEnvironment().withProperty("BETTER_GAME_TRACKER_DATABASE_PATH", database.toString());
        environment.setActiveProfiles("local");
        environment.getPropertySources().addLast(new MapPropertySource("localYaml", propertiesFrom("application-local.yml")));
        return environment;
    }

    @Test
    void rejectsAmbiguousOrMissingDeploymentProfiles() {
        var processor = new com.bettergametracker.config.LocalDatabasePathEnvironmentPostProcessor();
        var environment = new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles("local", "hosted");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
        environment.setActiveProfiles("other");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
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
