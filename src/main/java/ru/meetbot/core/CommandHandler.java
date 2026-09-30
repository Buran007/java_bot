package ru.meetbot.core;

import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CommandHandler {
    private final DatingRepository repository;
    private final Map<Long, ProfileDraft> drafts = new HashMap<>();
    private final Map<Long, Long> currentCandidates = new HashMap<>();
    private final Map<Long, Set<Long>> seenCandidates = new HashMap<>();

    public CommandHandler(DatingRepository repository) {
        this.repository = repository;
    }

    public synchronized String handle(long telegramId, String username, String text) {
        if (telegramId <= 0) return "Не удалось определить пользователя.";
        String message = text == null ? "" : text.strip();
        if (message.isEmpty()) return "Напишите команду. Список команд: /help";
        String command = message.startsWith("/") ? message.substring(1).toLowerCase(Locale.ROOT) : "";
        int botMention = command.indexOf('@');
        if (botMention >= 0) command = command.substring(0, botMention);

        if (!command.isEmpty()) {
            return switch (command) {
                case "start" -> start(telegramId, username, false);
                case "edit" -> start(telegramId, username, true);
                case "help" -> help();
                case "cancel" -> cancel(telegramId);
                case "profile" -> showProfile(telegramId);
                case "browse" -> browse(telegramId);
                case "like" -> like(telegramId);
                case "skip" -> browse(telegramId);
                case "matches" -> matches(telegramId);
                case "delete" -> delete(telegramId);
                case "health" -> repository.isHealthy() ? "OK" : "PostgreSQL недоступен.";
                case "info" -> "MeetBot помогает сотрудникам находить новые знакомства по взаимным симпатиям.";
                default -> "Неизвестная команда. Введите /help.";
            };
        }

        Optional<ProfileDraft> draft = Optional.ofNullable(drafts.get(telegramId));
        if (draft.isEmpty()) return "Сначала начните с /start. Список команд: /help";
        return acceptProfileAnswer(draft.get(), message);
    }

    private String start(long id, String username, boolean edit) {
        if (!edit && repository.findProfile(id).isPresent()) return "С возвращением! Используйте /browse, чтобы посмотреть анкеты.";
        ProfileDraft draft = drafts.computeIfAbsent(id, ignored -> ProfileDraft.first(id, username));
        return prompt(draft.step());
    }

    private String acceptProfileAnswer(ProfileDraft draft, String answer) {
        try {
            return switch (draft.step()) {
                case NAME -> {
                    ProfileDraft updated = draft.withName(answer);
                    drafts.put(draft.telegramId(), updated);
                    yield prompt(updated.step());
                }
                case AGE -> {
                    int age = Integer.parseInt(answer);
                    if (age < 18 || age > 120) yield "Укажите возраст от 18 до 120 лет.";
                    ProfileDraft updated = draft.withAge(age);
                    drafts.put(draft.telegramId(), updated);
                    yield prompt(updated.step());
                }
                case CITY -> {
                    ProfileDraft updated = draft.withCity(answer);
                    drafts.put(draft.telegramId(), updated);
                    yield prompt(updated.step());
                }
                case ABOUT -> {
                    Profile profile = draft.toProfile(answer);
                    repository.saveProfile(profile);
                    drafts.remove(profile.telegramId());
                    yield "Анкета создана! Теперь можно открыть /browse и посмотреть анкеты.";
                }
            };
        } catch (NumberFormatException e) {
            return "Возраст нужно указать числом, например 28.";
        } catch (IllegalArgumentException e) {
            return "Ответ слишком длинный или пустой. Попробуйте ещё раз.";
        }
    }

    private String browse(long id) {
        if (repository.findProfile(id).isEmpty()) return "Сначала создайте анкету: /start";
        Optional<Profile> candidate = repository.nextCandidate(id, seenCandidates.computeIfAbsent(id, ignored -> new HashSet<>()));
        if (candidate.isEmpty()) {
            currentCandidates.remove(id);
            return "Пока больше нет новых анкет. Попробуйте позже.";
        }
        Profile profile = candidate.get();
        seenCandidates.get(id).add(profile.telegramId());
        currentCandidates.put(id, profile.telegramId());
        return format(profile) + "\n\n/like — нравится, /skip — пропустить.";
    }

    private String like(long id) {
        Optional<Long> candidateId = Optional.ofNullable(currentCandidates.remove(id));
        if (candidateId.isEmpty()) return "Сначала откройте анкету командой /browse.";
        boolean newLike = repository.addLike(id, candidateId.get());
        if (!newLike) return "Эта анкета уже отмечена. Откройте следующую: /browse.";
        if (repository.findMatches(id, 1).stream().anyMatch(p -> p.telegramId() == candidateId.get())) {
            return "У вас взаимная симпатия! Совпадение с «" + repository.findProfile(candidateId.get()).orElseThrow().name()
                    + "». Откройте /matches, чтобы посмотреть совпадения.";
        }
        return "Отметка поставлена. Откройте следующую анкету: /browse.";
    }

    private String matches(long id) {
        var found = repository.findMatches(id, 10);
        if (found.isEmpty()) return "Совпадений пока нет. Продолжайте смотреть анкеты через /browse.";
        StringBuilder result = new StringBuilder("Ваши совпадения:");
        for (Profile profile : found) {
            result.append("\n• ").append(profile.name()).append(" — ").append(profile.city());
            if (profile.username() != null) result.append(" — @").append(profile.username());
        }
        return result.toString();
    }

    private String showProfile(long id) {
        return repository.findProfile(id).map(CommandHandler::format)
                .orElse("Анкета ещё не создана. Начните с /start.");
    }

    private String cancel(long id) {
        drafts.remove(id);
        return "Создание анкеты отменено. Чтобы начать заново, отправьте /start.";
    }

    private String delete(long id) {
        repository.deleteProfile(id);
        drafts.remove(id);
        currentCandidates.remove(id);
        seenCandidates.remove(id);
        return "Анкета и связанные с ней данные удалены.";
    }

    private static String prompt(ProfileDraft.Step step) {
        return switch (step) {
            case NAME -> "Давайте создадим анкету. Как вас называть?";
            case AGE -> "Сколько вам лет? Для участия нужно быть совершеннолетним.";
            case CITY -> "В каком городе вы находитесь?";
            case ABOUT -> "Расскажите немного о себе (до 300 символов).";
        };
    }

    private static String format(Profile p) {
        return p.name() + ", " + p.age() + " — " + p.city() + "\n" + p.about();
    }

    private static String help() {
        return "Команды:\n/start — создать анкету\n/edit — изменить анкету\n/profile — моя анкета\n"
                + "/browse — следующая анкета\n/like — нравится\n/skip — пропустить\n/matches — взаимные симпатии\n"
                + "/delete — удалить анкету\n/info — о боте\n/health — проверка БД\n/cancel — отменить создание\n/help — справка";
    }
}
