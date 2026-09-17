package com.bettergametracker.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bettergametracker.BetterGameTrackerApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.NativeDetector;

public final class LocalDatabasePathResolver {

    public Path resolve(String configuredPath) throws IOException {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return resolve(configuredPath, null);
        }
        boolean nativeRuntime = NativeDetector.inNativeImage();
        File source = nativeRuntime ? null : new ApplicationHome(BetterGameTrackerApplication.class).getSource();
        Path location = nativeRuntime
                ? ProcessHandle.current().info().command().map(Path::of).orElse(null)
                : source == null ? null : source.toPath();
        return resolve(configuredPath, applicationDirectory(location, nativeRuntime, Path.of("")));
    }

    public Path applicationDirectory(Path source, boolean nativeRuntime, Path workingDirectory) throws IOException {
        if (source == null || !source.isAbsolute()) {
            throw new IOException("Cannot determine absolute application location: " + source);
        }
        Path location = source.toRealPath();
        if (Files.isRegularFile(location)
                && (nativeRuntime || location.getFileName().toString().endsWith(".jar"))) {
            return location.getParent();
        }
        // A class directory is an explicit development launch, not a packaged-location fallback.
        if (!nativeRuntime && Files.isDirectory(location)
                && Files.isRegularFile(location.resolve("com/bettergametracker/BetterGameTrackerApplication.class"))) {
            return workingDirectory.toAbsolutePath().normalize();
        }
        throw new IOException("Unrecognized application location: " + source);
    }

    public Path resolve(String configuredPath, Path applicationDirectory) throws IOException {
        Path databasePath = (configuredPath == null || configuredPath.isBlank()
                ? applicationDirectory.resolve("db/better-game-tracker.db")
                : Path.of(configuredPath)).toAbsolutePath().normalize();
        Files.createDirectories(databasePath.getParent());
        if (Files.exists(databasePath) && (!Files.isRegularFile(databasePath) || !Files.isWritable(databasePath))) {
            throw new IOException("Local database is not a writable file: " + databasePath);
        }
        return databasePath;
    }
}
