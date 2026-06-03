package com.diplomawork.agents.db;

/**
 * Database connection settings for the Java agent layer.
 *
 * Environment variables are optional for local development:
 * DB_JDBC_URL, DB_USER, DB_PASSWORD.
 */
public final class DatabaseConfig {
    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:5432/tasks";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "";

    private DatabaseConfig() {
    }

    public static String jdbcUrl() {
        return valueOrDefault("DB_JDBC_URL", DEFAULT_JDBC_URL);
    }

    public static String user() {
        return valueOrDefault("DB_USER", DEFAULT_USER);
    }

    public static String password() {
        return valueOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private static String valueOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
