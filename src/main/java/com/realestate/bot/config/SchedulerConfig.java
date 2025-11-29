package com.realestate.bot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация планировщика для автоматической проверки новых объявлений
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Планировщик включен через @EnableScheduling
    // Интервал настроен в application.yml: scheduler.check-interval
}
