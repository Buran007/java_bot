package ru.meetbot;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Bot bot = new Bot();
        System.out.println("MeetBot запущен. Введите help; для завершения — exit.");

        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("> ");
                if (!input.hasNextLine()) {
                    break;
                }

                String command = input.nextLine().strip();
                if (command.isEmpty()) {
                    continue;
                }
                if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("/exit")) {
                    break;
                }

                System.out.println(bot.reply(command));
            }
        }

        System.out.println("MeetBot остановлен.");
    }
}
