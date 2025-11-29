package com.realestate.bot.config;

import com.realestate.bot.telegram.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Конфигурация для регистрации Telegram бота
 */
@Configuration
@Slf4j
public class TelegramBotConfig {

    /**
     * Регистрация бота в Telegram API
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) {
        TelegramBotsApi botsApi = null;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            log.info("Telegram bot successfully registered: {}", telegramBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Error registering Telegram bot", e);
            throw new RuntimeException("Failed to register Telegram bot", e);
        }
        return botsApi;
    }
}
