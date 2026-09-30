package ru.meetbot.core;

public record ProfileDraft(long telegramId, String username, Step step, String name, Integer age, String city) {
    public enum Step { NAME, AGE, CITY, ABOUT }

    public static ProfileDraft first(long telegramId, String username) {
        return new ProfileDraft(telegramId, username, Step.NAME, null, null, null);
    }

    public ProfileDraft withName(String value) {
        return new ProfileDraft(telegramId, username, Step.AGE, checked(value, 60), age, city);
    }

    public ProfileDraft withAge(int value) {
        return new ProfileDraft(telegramId, username, Step.CITY, name, value, city);
    }

    public ProfileDraft withCity(String value) {
        return new ProfileDraft(telegramId, username, Step.ABOUT, name, age, checked(value, 80));
    }

    public Profile toProfile(String about) {
        return new Profile(telegramId, username, name, age, city, about);
    }

    private static String checked(String value, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new IllegalArgumentException("invalid answer length");
        }
        return value.strip();
    }
}
