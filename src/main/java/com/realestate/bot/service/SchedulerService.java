package com.realestate.bot.service;

import com.realestate.bot.model.dto.ListingDto;
import com.realestate.bot.model.entity.Search;
import com.realestate.bot.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для периодической проверки новых объявлений
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final SearchService searchService;
    private final ListingService listingService;
    private final TelegramService telegramService;
    private final TelegramBot telegramBot;

    /**
     * Проверить новые объявления для всех активных поисков
     * Выполняется каждые 15 минут (900000 мс)
     */
    @Scheduled(fixedRate = 900000, initialDelay = 900000)
    public void checkNewListings() {
        log.info("Starting scheduled check for new listings");

        try {
            // Получаем все активные поиски
            List<Search> activeSearches = searchService.findAllActive();

            log.info("Found {} active searches to check", activeSearches.size());

            // Проверяем каждый поиск
            for (Search search : activeSearches) {
                try {
                    checkSearchForNewListings(search);
                } catch (Exception e) {
                    log.error("Error checking search {}", search.getId(), e);
                }
            }

            log.info("Completed scheduled check for new listings");

        } catch (Exception e) {
            log.error("Error in scheduled check", e);
        }
    }

    /**
     * Проверить новые объявления для конкретного поиска
     */
    private void checkSearchForNewListings(Search search) {
        log.debug("Checking search {} for user {}", search.getId(), search.getUser().getTelegramId());

        try {
            // Получаем новые объявления
            List<ListingDto> newListings = listingService.getNewListings(search);

            if (newListings.isEmpty()) {
                log.debug("No new listings found for search {}", search.getId());
                // Обновляем время последней проверки
                searchService.updateLastChecked(search.getId());
                return;
            }

            log.info("Found {} new listings for search {}", newListings.size(), search.getId());

            // Отправляем объявления пользователю
            Long chatId = search.getUser().getTelegramId();

            // Отправляем уведомление о найденных новых объявлениях
            String notificationMessage = String.format(
                    "🔔 Новые объявления!\n\n" +
                    "Найдено %d %s по вашему поиску:",
                    newListings.size(),
                    getRussianPluralForm(newListings.size(), "квартира", "квартиры", "квартир")
            );
            telegramService.sendMessage(chatId, notificationMessage, telegramBot);

            // Отправляем сами объявления
            int sentCount = telegramService.sendListings(chatId, newListings, telegramBot);

            // Отмечаем отправленные объявления
            if (sentCount > 0) {
                listingService.markAsSent(search, newListings.subList(0, sentCount));
                log.info("Sent {} listings to user {}", sentCount, chatId);
            }

            // Обновляем время последней проверки
            searchService.updateLastChecked(search.getId());

        } catch (Exception e) {
            log.error("Error checking search {} for new listings", search.getId(), e);
        }
    }

    /**
     * Получить правильную форму множественного числа для русского языка
     */
    private String getRussianPluralForm(int count, String form1, String form2, String form5) {
        int mod10 = count % 10;
        int mod100 = count % 100;

        if (mod10 == 1 && mod100 != 11) {
            return form1;
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            return form2;
        } else {
            return form5;
        }
    }

    /**
     * Ручная проверка новых объявлений (может быть вызвана по требованию)
     */
    public void checkNow() {
        log.info("Manual trigger for checking new listings");
        checkNewListings();
    }
}
