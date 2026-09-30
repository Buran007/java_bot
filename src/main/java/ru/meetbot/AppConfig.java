package ru.meetbot;

public record AppConfig(String botToken, String databaseUrl, String databaseUser, String databasePassword) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(required("TELEGRAM_BOT_TOKEN"), required("DATABASE_URL"),
                required("DATABASE_USER"), required("DATABASE_PASSWORD"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }
}
