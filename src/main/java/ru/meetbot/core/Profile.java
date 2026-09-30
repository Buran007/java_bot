package ru.meetbot.core;

public record Profile(long telegramId, String username, String name, int age, String city, String about) {
    public Profile {
        if (telegramId <= 0) throw new IllegalArgumentException("telegramId must be positive");
        username = username == null || username.isBlank() ? null : username.strip();
        name = required(name, 60);
        if (age < 18 || age > 120) throw new IllegalArgumentException("age must be between 18 and 120");
        city = required(city, 80);
        about = required(about, 300);
    }

    private static String required(String value, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new IllegalArgumentException("invalid value");
        }
        return value.strip();
    }
}
