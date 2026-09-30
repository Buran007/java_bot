package ru.meetbot.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DatingRepository {
    Optional<Profile> findProfile(long telegramId);
    void saveProfile(Profile profile);
    void deleteProfile(long telegramId);
    Optional<Profile> nextCandidate(long telegramId, Set<Long> seenIds);
    boolean addLike(long fromTelegramId, long toTelegramId);
    List<Profile> findMatches(long telegramId, int limit);
    boolean isHealthy();
}
