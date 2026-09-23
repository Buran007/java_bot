package ru.meetbot;

import java.time.Duration;
import java.util.Locale;

public final class Bot {
    private final long startedAt = System.nanoTime();

    public String reply(String input) {
        String command = normalize(input);
        return switch (command) {
            case "help" -> help();
            case "info" -> info();
            case "health" -> health();
            default -> "Неизвестная команда. Введите help.";
        };
    }

    private String normalize(String input) {
        String command = input.strip().toLowerCase(Locale.ROOT);
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private String help() {
        return """
                Доступные команды:
                  help   — показать эту справку
                  info   — рассказать о боте
                  health — проверить работу бота
                  exit   — завершить консольное приложение
                Можно использовать команды с /: например, /help.
                """.strip();
    }

    private String info() {
        return """
                MeetBot — бот для знакомства сотрудников.
                Режим работы: консоль.
                """.strip();
    }

    private String health() {
        long uptime = Duration.ofNanos(System.nanoTime() - startedAt).toSeconds();
        return "OK | uptime=" + uptime + "s | java=" + Runtime.version();
    }
}
