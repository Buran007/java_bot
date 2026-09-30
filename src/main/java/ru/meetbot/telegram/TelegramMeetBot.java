package ru.meetbot.telegram;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.meetbot.core.CommandHandler;

import java.util.List;

public final class TelegramMeetBot implements LongPollingUpdateConsumer {
    private final TelegramClient client;
    private final CommandHandler commands;

    public TelegramMeetBot(String token, CommandHandler commands) {
        this.client = new OkHttpTelegramClient(token);
        this.commands = commands;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::consumeOne);
    }

    private void consumeOne(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText() || update.getMessage().getFrom() == null) return;

        var message = update.getMessage();
        try {
            String answer = commands.handle(message.getFrom().getId(), message.getFrom().getUserName(), message.getText());
            client.execute(SendMessage.builder().chatId(message.getChatId()).text(answer).build());
        } catch (TelegramApiException | RuntimeException error) {
            System.err.println("Failed to process Telegram update: " + error.getMessage());
            try {
                client.execute(SendMessage.builder().chatId(message.getChatId())
                        .text("Не удалось обработать запрос. Попробуйте ещё раз.").build());
            } catch (TelegramApiException ignored) {
            }
        }
    }
}
