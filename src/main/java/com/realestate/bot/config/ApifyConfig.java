package com.realestate.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Apify API
 */
@Configuration
@ConfigurationProperties(prefix = "apify")
@Getter
@Setter
public class ApifyConfig {

    /**
     * API ключ Apify
     */
    private String apiKey;

    /**
     * Базовый URL Apify API (обычно https://api.apify.com/v2)
     */
    private String baseUrl;

    /**
     * ID актора Idealista Scraper (igolaizola~idealista-scraper)
     */
    private String actorId;

    /**
     * Таймаут для выполнения запроса (в секундах)
     */
    private int timeout;
}
