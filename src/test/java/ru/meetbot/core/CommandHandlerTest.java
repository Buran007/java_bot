package ru.meetbot.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerTest {
    private final MemoryRepository repository = new MemoryRepository();
    private final CommandHandler bot = new CommandHandler(repository);

    @Test
    void createsProfileThroughWizardAndRejectsInvalidAge() {
        assertTrue(bot.handle(1, "alex", "/start").contains("Как вас называть"));
        assertTrue(bot.handle(1, "alex", "Alex").contains("Сколько вам лет"));
        assertTrue(bot.handle(1, "alex", "17").contains("от 18"));
        bot.handle(1, "alex", "28");
        bot.handle(1, "alex", "Казань");
        assertTrue(bot.handle(1, "alex", "Люблю походы").contains("Анкета создана"));
        assertEquals(new Profile(1, "alex", "Alex", 28, "Казань", "Люблю походы"), repository.findProfile(1).orElseThrow());
    }

    @Test
    void matchingRequiresBothUsersToLike() {
        repository.saveProfile(new Profile(1, "alex", "Alex", 28, "Казань", "Походы"));
        repository.saveProfile(new Profile(2, "sam", "Sam", 30, "Пермь", "Книги"));
        repository.addLike(2, 1);

        assertTrue(bot.handle(1, "alex", "/browse").contains("Sam, 30"));
        assertTrue(bot.handle(1, "alex", "/like").contains("взаимная симпатия"));
        assertEquals(List.of(repository.findProfile(2).orElseThrow()), repository.findMatches(1, 10));
        assertTrue(bot.handle(1, "alex", "/matches").contains("@sam"));
    }

    @Test
    void reportsHealthAndDeletesProfile() {
        repository.saveProfile(new Profile(1, "alex", "Alex", 28, "Казань", "Походы"));
        assertEquals("OK", bot.handle(1, "alex", "/health"));
        assertTrue(bot.handle(1, "alex", "/delete").contains("удалены"));
        assertTrue(repository.findProfile(1).isEmpty());
    }

    private static final class MemoryRepository implements DatingRepository {
        private final Map<Long, Profile> profiles = new HashMap<>();
        private final Map<Long, Set<Long>> likes = new HashMap<>();

        public Optional<Profile> findProfile(long id) { return Optional.ofNullable(profiles.get(id)); }
        public void saveProfile(Profile profile) { profiles.put(profile.telegramId(), profile); }
        public void deleteProfile(long id) { profiles.remove(id); }
        public boolean isHealthy() { return true; }
        public Optional<Profile> nextCandidate(long id, Set<Long> seenIds) {
            Set<Long> exclusions = new HashSet<>(seenIds);
            exclusions.addAll(likes.getOrDefault(id, Set.of()));
            return profiles.values().stream()
                    .filter(p -> p.telegramId() != id && !exclusions.contains(p.telegramId())).findFirst();
        }

        public boolean addLike(long from, long to) { return likes.computeIfAbsent(from, ignored -> new HashSet<>()).add(to); }

        public List<Profile> findMatches(long id, int limit) {
            return likes.getOrDefault(id, Set.of()).stream()
                    .filter(other -> likes.getOrDefault(other, Set.of()).contains(id))
                    .map(profiles::get).filter(p -> p != null).limit(limit).toList();
        }
    }
}
