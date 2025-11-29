package com.realestate.bot.telegram.handler;

import com.realestate.bot.model.dto.SearchCriteriaDto;
import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.enums.ConversationState;
import com.realestate.bot.service.SearchService;
import com.realestate.bot.telegram.keyboard.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик текстовых сообщений (не команд)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final KeyboardFactory keyboardFactory;
    private final SearchService searchService;

    // Хранилище состояний диалога (chatId -> ConversationState)
    private final Map<Long, ConversationState> userStates = new ConcurrentHashMap<>();

    // Временное хранилище критериев поиска в процессе создания (chatId -> SearchCriteriaDto)
    private final Map<Long, SearchCriteriaDto> tempCriteria = new ConcurrentHashMap<>();

    // Хранилище ID редактируемого поиска (chatId -> searchId)
    private final Map<Long, Long> editingSearchId = new ConcurrentHashMap<>();

    /**
     * Обработать текстовое сообщение
     */
    public void handle(Update update, AbsSender sender) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Получаем текущее состояние пользователя
        ConversationState state = userStates.getOrDefault(chatId, ConversationState.NONE);

        log.debug("Processing text message from chatId: {}, state: {}, text: {}", chatId, state, text);

        switch (state) {
            case WAITING_MIN_PRICE -> handleMinPrice(chatId, text, sender);
            case WAITING_MAX_PRICE -> handleMaxPrice(chatId, text, sender);
            case EDITING_MIN_PRICE -> handleEditingMinPrice(chatId, text, sender);
            case EDITING_MAX_PRICE -> handleEditingMaxPrice(chatId, text, sender);
            case NONE -> handleNoState(chatId, sender);
            default -> log.warn("Unknown conversation state: {}", state);
        }
    }

    /**
     * Обработка ввода минимальной цены
     */
    private void handleMinPrice(Long chatId, String text, AbsSender sender) {
        try {
            int minPrice = Integer.parseInt(text.trim());

            if (minPrice < 0) {
                sendMessage(chatId, "❌ Цена не может быть отрицательной. Введите минимальную цену (EUR):", sender);
                return;
            }

            // Сохраняем минимальную цену
            SearchCriteriaDto criteria = tempCriteria.computeIfAbsent(chatId, k -> new SearchCriteriaDto());
            criteria.setMinPrice(minPrice);

            log.debug("Min price set for chatId {}: {}", chatId, minPrice);

            // Переходим к следующему шагу
            userStates.put(chatId, ConversationState.WAITING_MAX_PRICE);
            sendMessage(chatId, "Введите максимальную цену (EUR):", sender);

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Неверный формат. Введите число (например, 500):", sender);
        }
    }

    /**
     * Обработка ввода максимальной цены
     */
    private void handleMaxPrice(Long chatId, String text, AbsSender sender) {
        try {
            int maxPrice = Integer.parseInt(text.trim());

            SearchCriteriaDto criteria = tempCriteria.get(chatId);
            if (criteria == null) {
                resetConversation(chatId);
                sendMessage(chatId, "❌ Произошла ошибка. Начните создание поиска заново.", sender);
                return;
            }

            if (maxPrice < 0) {
                sendMessage(chatId, "❌ Цена не может быть отрицательной. Введите максимальную цену (EUR):", sender);
                return;
            }

            if (criteria.getMinPrice() != null && maxPrice <= criteria.getMinPrice()) {
                sendMessage(chatId,
                        String.format("❌ Максимальная цена должна быть больше минимальной (%d EUR).\n" +
                                "Введите максимальную цену:", criteria.getMinPrice()), sender);
                return;
            }

            // Сохраняем максимальную цену
            criteria.setMaxPrice(maxPrice);

            log.debug("Max price set for chatId {}: {}", chatId, maxPrice);

            // Переходим к выбору количества комнат
            userStates.put(chatId, ConversationState.WAITING_NUM_ROOMS);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Сколько комнат вы ищете?");
            message.setReplyMarkup(keyboardFactory.createRoomSelection());

            try {
                sender.execute(message);
            } catch (TelegramApiException e) {
                log.error("Error sending room selection keyboard to chatId: {}", chatId, e);
            }

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Неверный формат. Введите число (например, 1200):", sender);
        }
    }

    /**
     * Обработка сообщения когда нет активного состояния
     */
    private void handleNoState(Long chatId, AbsSender sender) {
        sendMessage(chatId,
                "Используйте команды для управления ботом:\n" +
                "/start - Главное меню\n" +
                "/mysearch - Мой поиск\n" +
                "/help - Справка", sender);
    }

    /**
     * Начать создание поиска
     */
    public void startSearchCreation(Long chatId, AbsSender sender) {
        log.info("Starting search creation for chatId: {}", chatId);

        // Инициализируем временное хранилище
        tempCriteria.put(chatId, new SearchCriteriaDto());
        tempCriteria.get(chatId).setDistricts(new ArrayList<>());

        // Устанавливаем состояние
        userStates.put(chatId, ConversationState.WAITING_MIN_PRICE);

        sendMessage(chatId, "Начинаем создание поиска!\n\nВведите минимальную цену (EUR):", sender);
    }

    /**
     * Получить временные критерии поиска для chatId
     */
    public SearchCriteriaDto getTempCriteria(Long chatId) {
        return tempCriteria.get(chatId);
    }

    /**
     * Установить временные критерии поиска для chatId
     */
    public void setTempCriteria(Long chatId, SearchCriteriaDto criteria) {
        tempCriteria.put(chatId, criteria);
    }

    /**
     * Установить состояние диалога
     */
    public void setState(Long chatId, ConversationState state) {
        userStates.put(chatId, state);
    }

    /**
     * Получить текущее состояние диалога
     */
    public ConversationState getState(Long chatId) {
        return userStates.getOrDefault(chatId, ConversationState.NONE);
    }

    /**
     * Сбросить состояние диалога
     */
    public void resetConversation(Long chatId) {
        userStates.remove(chatId);
        tempCriteria.remove(chatId);
        editingSearchId.remove(chatId);
        log.debug("Conversation reset for chatId: {}", chatId);
    }

    /**
     * Установить ID редактируемого поиска
     */
    public void setEditingSearchId(Long chatId, Long searchId) {
        editingSearchId.put(chatId, searchId);
    }

    /**
     * Получить ID редактируемого поиска
     */
    public Long getEditingSearchId(Long chatId) {
        return editingSearchId.get(chatId);
    }

    /**
     * Обработка ввода минимальной цены при редактировании
     */
    private void handleEditingMinPrice(Long chatId, String text, AbsSender sender) {
        Long searchId = editingSearchId.get(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            resetConversation(chatId);
            return;
        }

        try {
            int minPrice = Integer.parseInt(text.trim());

            if (minPrice < 0) {
                sendMessage(chatId, "❌ Цена не может быть отрицательной. Введите минимальную цену (EUR):", sender);
                return;
            }

            // Получаем поиск и сохраняем минимальную цену во временное хранилище
            Search search = searchService.findById(searchId);
            SearchCriteriaDto criteria = searchService.toDto(search);
            criteria.setMinPrice(minPrice);

            // Сохраняем во временное хранилище
            tempCriteria.put(chatId, criteria);

            log.debug("Editing min price set for chatId {}: {}", chatId, minPrice);

            // Переходим к вводу максимальной цены
            userStates.put(chatId, ConversationState.EDITING_MAX_PRICE);
            sendMessage(chatId, "Введите новую максимальную цену (EUR):", sender);

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Неверный формат. Введите число (например, 500):", sender);
        } catch (Exception e) {
            log.error("Error handling editing min price", e);
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            resetConversation(chatId);
        }
    }

    /**
     * Обработка ввода максимальной цены при редактировании
     */
    private void handleEditingMaxPrice(Long chatId, String text, AbsSender sender) {
        Long searchId = editingSearchId.get(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            resetConversation(chatId);
            return;
        }

        try {
            int maxPrice = Integer.parseInt(text.trim());

            SearchCriteriaDto criteria = tempCriteria.get(chatId);
            if (criteria == null) {
                sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
                resetConversation(chatId);
                return;
            }

            if (maxPrice < 0) {
                sendMessage(chatId, "❌ Цена не может быть отрицательной. Введите максимальную цену (EUR):", sender);
                return;
            }

            if (criteria.getMinPrice() != null && maxPrice <= criteria.getMinPrice()) {
                sendMessage(chatId,
                        String.format("❌ Максимальная цена должна быть больше минимальной (%d EUR).\n" +
                                "Введите максимальную цену:", criteria.getMinPrice()), sender);
                return;
            }

            // Сохраняем максимальную цену
            criteria.setMaxPrice(maxPrice);

            log.debug("Editing max price set for chatId {}: {}", chatId, maxPrice);

            // Обновляем поиск
            searchService.updateCriteria(searchId, criteria);

            // Очищаем состояние
            resetConversation(chatId);

            sendMessage(chatId,
                    String.format("✅ Цены обновлены: %,d - %,d EUR\n\nИзменения сохранены!",
                            criteria.getMinPrice(), criteria.getMaxPrice()), sender);

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Неверный формат. Введите число (например, 1200):", sender);
        } catch (Exception e) {
            log.error("Error handling editing max price", e);
            sendMessage(chatId, "❌ Произошла ошибка при сохранении изменений", sender);
            resetConversation(chatId);
        }
    }

    /**
     * Отправить текстовое сообщение
     */
    private void sendMessage(Long chatId, String text, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chatId: {}", chatId, e);
        }
    }
}
