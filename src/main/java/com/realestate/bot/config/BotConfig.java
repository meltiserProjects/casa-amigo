package com.realestate.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Telegram бота
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter
@Setter
public class BotConfig {

    /**
     * Токен Telegram бота от @BotFather
     */
    private String token;

    /**
     * Username бота (например, @ValenciaRentBot)
     */
    private String username;
}
