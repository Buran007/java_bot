package ru.meetbot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.meetbot.core.CommandHandler;
import ru.meetbot.postgres.JdbcDatingRepository;
import ru.meetbot.telegram.TelegramMeetBot;

import java.util.concurrent.CountDownLatch;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.databaseUrl());
        hikari.setUsername(config.databaseUser());
        hikari.setPassword(config.databasePassword());
        hikari.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(hikari);
             TelegramBotsLongPollingApplication telegram = new TelegramBotsLongPollingApplication()) {
            Flyway.configure().dataSource(dataSource).load().migrate();
            var commands = new CommandHandler(new JdbcDatingRepository(dataSource));
            telegram.registerBot(config.botToken(), new TelegramMeetBot(config.botToken(), commands));
            System.out.println("MeetBot started");
            new CountDownLatch(1).await();
        }
    }
}
