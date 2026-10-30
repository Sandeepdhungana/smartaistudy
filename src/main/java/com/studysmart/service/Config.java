package com.studysmart.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reads settings from environment variables, falling back to a .env file in the project folder
public final class Config {
    private static final Map<String, String> DOT_ENV = loadDotEnv(Path.of(".env"));

    private Config() {
    }

    public static String get(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = DOT_ENV.get(name);
        }
        return value;
    }

    // Each line is KEY=value; blank lines and lines starting with # are skipped
    static Map<String, String> loadDotEnv(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return values;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return values;
        }
        for (String line : lines) {
            line = line.strip();
            if (line.startsWith("export ")) {
                line = line.substring(7).strip();
            }
            int eq = line.indexOf('=');
            if (line.isEmpty() || line.startsWith("#") || eq <= 0) {
                continue;
            }
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(line.substring(0, eq).strip(), value);
        }
        return values;
    }
}
