package com.bittercode.util;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.cdimascio.dotenv.Dotenv;

public final class EmailConfig {

    private static volatile Dotenv dotenv = loadFromAvailableLocation(null);
    private static volatile String loadedFrom = "environment variables or default working directory";

    private EmailConfig() {
    }

    /**
     * Called at application startup so a local .env in the project/web-app
     * hierarchy is found even when Tomcat has a different working directory.
     */
    public static synchronized void initialize(String webAppRoot) {
        dotenv = loadFromAvailableLocation(webAppRoot);
        System.out.println("[EmailConfig] SMTP configuration source: " + loadedFrom);
    }

    public static String get(String key, String defaultValue) {
        String environmentValue = System.getenv(key);
        if (!isBlank(environmentValue)) {
            return environmentValue;
        }
        return dotenv.get(key, defaultValue);
    }

    public static boolean isConfigured() {
        return !isBlank(get("SMTP_HOST", null)) && !isBlank(get("SMTP_FROM", null));
    }

    private static Dotenv loadFromAvailableLocation(String webAppRoot) {
        Set<Path> directories = new LinkedHashSet<>();
        addDirectoryAndParents(directories, pathFromEnvironment("EMAIL_ENV_FILE"));
        addDirectoryAndParents(directories, pathOrNull(webAppRoot));
        addDirectoryAndParents(directories, pathOrNull(System.getProperty("user.dir")));
        addCodeSourceDirectories(directories);

        for (Path directory : directories) {
            if (Files.isRegularFile(directory.resolve(".env"))) {
                loadedFrom = directory.resolve(".env").toAbsolutePath().toString();
                return Dotenv.configure()
                        .directory(directory.toAbsolutePath().toString())
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
            }
        }

        loadedFrom = "environment variables (no .env file found)";
        return Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
    }

    private static void addDirectoryAndParents(Set<Path> directories, Path path) {
        if (path == null) return;
        Path directory = Files.isDirectory(path) ? path : path.getParent();
        for (int depth = 0; directory != null && depth < 6; depth++, directory = directory.getParent()) {
            directories.add(directory);
        }
    }

    private static Path pathFromEnvironment(String key) {
        String value = System.getenv(key);
        return isBlank(value) ? null : pathOrNull(value);
    }

    private static Path pathOrNull(String value) {
        if (isBlank(value)) return null;
        try {
            return Paths.get(value).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addCodeSourceDirectories(Set<Path> directories) {
        try {
            Path location = Paths.get(EmailConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            addDirectoryAndParents(directories, location);
        } catch (URISyntaxException | SecurityException ignored) {
            // The normal working-directory and web-app-root lookups remain available.
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
