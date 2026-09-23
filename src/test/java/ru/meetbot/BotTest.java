package ru.meetbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotTest {
    private final Bot bot = new Bot();

    @Test
    void helpListsSupportedCommands() {
        String reply = bot.reply("help");
        for (String command : new String[]{"help", "info", "health", "exit"}) {
            assertTrue(reply.contains(command));
        }
    }

    @Test
    void acceptsSlashWhitespaceAndDifferentCase() {
        assertEquals(bot.reply("help"), bot.reply("  /HeLp  "));
        assertEquals(bot.reply("info"), bot.reply(" INFO "));
    }

    @Test
    void infoExplainsCurrentScope() {
        assertTrue(bot.reply("info").contains("Режим работы: консоль."));
    }

    @Test
    void healthReportsStatusUptimeAndJava() {
        assertTrue(bot.reply("/health").matches("OK \\| uptime=\\d+s \\| java=.+"));
    }

    @Test
    void unsupportedInputSuggestsHelp() {
        for (String command : new String[]{"unknown", "", "/", "help extra"}) {
            assertEquals("Неизвестная команда. Введите help.", bot.reply(command));
        }
    }
}
