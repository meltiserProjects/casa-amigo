package com.realestate.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс Spring Boot приложения для Telegram-бота поиска недвижимости
 */
@SpringBootApplication
@EnableScheduling  // Включаем поддержку планировщика задач
public class RealEstateBotApplication {

    public static void main(String[] args) {
        // Загружаем переменные окружения из .env файла
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .ignoreIfMissing()  // Не падать если .env отсутствует
                    .load();

            // Устанавливаем системные свойства из .env
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }

        SpringApplication.run(RealEstateBotApplication.class, args);
    }
}
