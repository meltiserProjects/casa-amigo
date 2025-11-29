package com.realestate.bot.telegram.handler;

import com.realestate.bot.exception.SearchLimitException;
import com.realestate.bot.model.dto.ListingDto;
import com.realestate.bot.model.dto.SearchCriteriaDto;
import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.entity.User;
import com.realestate.bot.model.enums.ConversationState;
import com.realestate.bot.model.enums.SearchStatus;
import com.realestate.bot.service.ListingService;
import com.realestate.bot.service.SearchService;
import com.realestate.bot.service.TelegramService;
import com.realestate.bot.service.UserService;
import com.realestate.bot.telegram.keyboard.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Обработчик callback запросов (нажатий на inline кнопки)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackQueryHandler {

    private final SearchService searchService;
    private final UserService userService;
    private final KeyboardFactory keyboardFactory;
    private final MessageHandler messageHandler;
    private final ListingService listingService;
    private final TelegramService telegramService;

    /**
     * Обработать callback запрос
     */
    public void handle(Update update, AbsSender sender) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackQueryId = callbackQuery.getId();

        log.debug("Processing callback: {} from chatId: {}", callbackData, chatId);

        // Разбираем callback data (формат: ACTION или ACTION:DATA)
        String[] parts = callbackData.split(":", 2);
        String action = parts[0];
        String data = parts.length > 1 ? parts[1] : null;

        try {
            switch (action) {
                case "CREATE_SEARCH" -> handleCreateSearch(chatId, sender);
                case "MY_SEARCH" -> handleMySearch(chatId, sender);
                case "HELP" -> handleHelp(chatId, sender);
                case "SET_ROOMS" -> handleSetRooms(chatId, data, sender);
                case "TOGGLE_DISTRICT" -> handleToggleDistrict(chatId, data, sender);
                case "DISTRICTS_ALL" -> handleDistrictsAll(chatId, sender);
                case "DISTRICTS_DONE" -> handleDistrictsDone(chatId, sender);
                case "PAUSE_SEARCH" -> handlePauseSearch(chatId, sender);
                case "RESUME_SEARCH" -> handleResumeSearch(chatId, sender);
                case "EDIT_SEARCH" -> handleEditSearch(chatId, sender);
                case "EDIT_PRICE" -> handleEditPrice(chatId, sender);
                case "EDIT_ROOMS" -> handleEditRooms(chatId, sender);
                case "EDIT_DISTRICTS" -> handleEditDistricts(chatId, sender);
                case "EDIT_SET_ROOMS" -> handleEditSetRooms(chatId, data, sender);
                case "EDIT_TOGGLE_DISTRICT" -> handleEditToggleDistrict(chatId, data, sender);
                case "EDIT_DISTRICTS_ALL" -> handleEditDistrictsAll(chatId, sender);
                case "EDIT_DISTRICTS_DONE" -> handleEditDistrictsDone(chatId, sender);
                case "CANCEL_EDIT" -> handleCancelEdit(chatId, sender);
                case "DELETE_SEARCH" -> handleDeleteSearch(chatId, sender);
                case "CONFIRM_DELETE" -> handleConfirmDelete(chatId, sender);
                case "CANCEL_DELETE" -> handleCancelDelete(chatId, sender);
                case "BACK_TO_MAIN" -> handleBackToMain(chatId, sender);
                default -> log.warn("Unknown callback action: {}", action);
            }

            // Отправляем подтверждение callback
            answerCallbackQuery(callbackQueryId, sender);

        } catch (Exception e) {
            log.error("Error processing callback: {}", callbackData, e);
            answerCallbackQuery(callbackQueryId, "❌ Произошла ошибка", sender);
        }
    }

    /**
     * Начать создание поиска
     */
    private void handleCreateSearch(Long chatId, AbsSender sender) {
        // Проверяем нет ли уже активного поиска
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> existingSearch = searchService.getActiveSearch(user.getId());

        if (existingSearch.isPresent()) {
            sendMessage(chatId,
                    "❌ У вас уже есть активный поиск.\n\n" +
                    "Используйте /mysearch для управления им.", sender);
            return;
        }

        // Начинаем создание поиска
        messageHandler.startSearchCreation(chatId, sender);
    }

    /**
     * Просмотр моего поиска
     */
    private void handleMySearch(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId,
                    "У вас пока нет активного поиска.\n\n" +
                    "Создайте поиск, чтобы получать уведомления о новых квартирах!",
                    sender);
            return;
        }

        Search search = searchOpt.get();
        String info = searchService.formatSearchInfo(search);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(info);
        message.setReplyMarkup(keyboardFactory.createSearchManagement(
                search.getStatus() == SearchStatus.ACTIVE));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending search info to chatId: {}", chatId, e);
        }
    }

    /**
     * Показать справку
     */
    private void handleHelp(Long chatId, AbsSender sender) {
        String helpMessage = "❓ Помощь\n\n" +
                "Доступные команды:\n" +
                "/start - Начать работу с ботом\n" +
                "/mysearch - Посмотреть мой активный поиск\n" +
                "/help - Показать эту справку\n\n" +
                "Как пользоваться:\n" +
                "1. Создайте поиск с вашими критериями\n" +
                "2. Получите текущие предложения сразу\n" +
                "3. Бот будет присылать новые каждые 15 минут\n" +
                "4. Управляйте поиском: приостановить, редактировать, удалить";

        sendMessage(chatId, helpMessage, sender);
    }

    /**
     * Установка количества комнат
     */
    private void handleSetRooms(Long chatId, String data, AbsSender sender) {
        try {
            int numRooms = Integer.parseInt(data);

            SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
            if (criteria == null) {
                sendMessage(chatId, "❌ Произошла ошибка. Начните создание поиска заново.", sender);
                return;
            }

            criteria.setNumRooms(numRooms);
            log.debug("Rooms set for chatId {}: {}", chatId, numRooms);

            // Переходим к выбору районов
            messageHandler.setState(chatId, ConversationState.WAITING_DISTRICTS);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Выберите районы (можно несколько):");
            message.setReplyMarkup(keyboardFactory.createDistrictSelection(criteria.getDistricts()));

            try {
                sender.execute(message);
            } catch (TelegramApiException e) {
                log.error("Error sending district selection to chatId: {}", chatId, e);
            }

        } catch (NumberFormatException e) {
            log.error("Invalid rooms data: {}", data);
        }
    }

    /**
     * Переключение выбора района
     */
    private void handleToggleDistrict(Long chatId, String district, AbsSender sender) {
        SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
        if (criteria == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните создание поиска заново.", sender);
            return;
        }

        if (criteria.getDistricts() == null) {
            criteria.setDistricts(new ArrayList<>());
        }

        // Переключаем выбор района
        if (criteria.getDistricts().contains(district)) {
            criteria.getDistricts().remove(district);
            log.debug("District removed for chatId {}: {}", chatId, district);
        } else {
            criteria.getDistricts().add(district);
            log.debug("District added for chatId {}: {}", chatId, district);
        }

        // Обновляем клавиатуру (показываем галочки)
        // TODO: Implement message editing to show checkmarks immediately
        // For now, checkmarks will appear on next button press
        log.debug("District selection updated for chatId {}", chatId);
    }

    /**
     * Выбрать все районы
     */
    private void handleDistrictsAll(Long chatId, AbsSender sender) {
        SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
        if (criteria == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните создание поиска заново.", sender);
            return;
        }

        // Устанавливаем пустой список (означает "все районы")
        criteria.setDistricts(new ArrayList<>());
        log.debug("All districts selected for chatId: {}", chatId);

        // Завершаем создание поиска
        completeSearchCreation(chatId, sender);
    }

    /**
     * Завершить выбор районов
     */
    private void handleDistrictsDone(Long chatId, AbsSender sender) {
        SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
        if (criteria == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните создание поиска заново.", sender);
            return;
        }

        if (criteria.getDistricts() == null || criteria.getDistricts().isEmpty()) {
            sendMessage(chatId, "❌ Выберите хотя бы один район или нажмите \"Все районы\"", sender);
            return;
        }

        // Завершаем создание поиска
        completeSearchCreation(chatId, sender);
    }

    /**
     * Завершить создание поиска
     */
    private void completeSearchCreation(Long chatId, AbsSender sender) {
        SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
        User user = userService.getUserByTelegramId(chatId);

        try {
            // Создаем поиск
            Search search = searchService.createSearch(user.getId(), criteria);

            log.info("Search created successfully for chatId {}: searchId={}", chatId, search.getId());

            // Очищаем временные данные
            messageHandler.resetConversation(chatId);

            // Отправляем подтверждение
            sendMessage(chatId,
                    "✅ Поиск создан!\n\n" +
                    "🔍 Ищу актуальные предложения...", sender);

            // Получаем новые объявления
            List<ListingDto> newListings = listingService.getNewListings(search);

            if (newListings.isEmpty()) {
                sendMessage(chatId,
                        "К сожалению, по вашим критериям пока нет подходящих квартир.\n\n" +
                        "Буду проверять новые предложения каждые 15 минут и присылать вам уведомления.", sender);
            } else {
                sendMessage(chatId,
                        String.format("Найдено %d %s:\n",
                                newListings.size(),
                                getRussianPluralForm(newListings.size(), "квартира", "квартиры", "квартир")),
                        sender);

                // Отправляем объявления пользователю
                int sentCount = telegramService.sendListings(chatId, newListings, sender);

                // Отмечаем отправленные объявления
                if (sentCount > 0) {
                    listingService.markAsSent(search, newListings.subList(0, sentCount));
                }

                sendMessage(chatId,
                        "\n✅ Буду проверять новые предложения каждые 15 минут.", sender);
            }

            // Обновляем время последней проверки
            searchService.updateLastChecked(search.getId());

        } catch (SearchLimitException e) {
            sendMessage(chatId, "❌ " + e.getMessage(), sender);
            messageHandler.resetConversation(chatId);
        } catch (Exception e) {
            log.error("Error creating search for chatId: {}", chatId, e);
            sendMessage(chatId, "❌ Произошла ошибка при создании поиска. Попробуйте еще раз.", sender);
            messageHandler.resetConversation(chatId);
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
     * Приостановить поиск
     */
    private void handlePauseSearch(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет активного поиска", sender);
            return;
        }

        searchService.pauseSearch(searchOpt.get().getId());
        sendMessage(chatId,
                "⏸ Поиск приостановлен.\n\n" +
                "Уведомления остановлены. Вы можете возобновить поиск в любое время.", sender);
    }

    /**
     * Возобновить поиск
     */
    private void handleResumeSearch(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет поиска для возобновления", sender);
            return;
        }

        try {
            searchService.resumeSearch(searchOpt.get().getId());
            sendMessage(chatId,
                    "▶️ Поиск возобновлен!\n\n" +
                    "Снова проверяю новые предложения каждые 15 минут.", sender);
        } catch (SearchLimitException e) {
            sendMessage(chatId, "❌ " + e.getMessage(), sender);
        }
    }

    /**
     * Редактировать поиск
     */
    private void handleEditSearch(Long chatId, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Что вы хотите изменить?");
        message.setReplyMarkup(keyboardFactory.createEditOptions());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending edit options to chatId: {}", chatId, e);
        }
    }

    /**
     * Начать редактирование цены
     */
    private void handleEditPrice(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет активного поиска", sender);
            return;
        }

        Search search = searchOpt.get();
        messageHandler.setEditingSearchId(chatId, search.getId());

        // Показываем текущие значения
        String currentPrices = String.format("Текущий диапазон цен: %s - %s EUR",
                search.getMinPrice() != null ? String.format("%,d", search.getMinPrice()) : "не указано",
                search.getMaxPrice() != null ? String.format("%,d", search.getMaxPrice()) : "не указано");

        sendMessage(chatId, currentPrices + "\n\nВведите новую минимальную цену (EUR):", sender);
        messageHandler.setState(chatId, ConversationState.EDITING_MIN_PRICE);
    }

    /**
     * Начать редактирование количества комнат
     */
    private void handleEditRooms(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет активного поиска", sender);
            return;
        }

        Search search = searchOpt.get();
        messageHandler.setEditingSearchId(chatId, search.getId());

        // Показываем текущее значение
        String currentRooms = search.getNumRooms() != null
                ? String.format("Текущее количество комнат: %d\n\n", search.getNumRooms())
                : "Текущее количество комнат: не указано\n\n";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(currentRooms + "Выберите новое количество комнат:");
        message.setReplyMarkup(createEditRoomSelection());

        try {
            sender.execute(message);
            messageHandler.setState(chatId, ConversationState.EDITING_NUM_ROOMS);
        } catch (TelegramApiException e) {
            log.error("Error sending edit room selection to chatId: {}", chatId, e);
        }
    }

    /**
     * Начать редактирование районов
     */
    private void handleEditDistricts(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет активного поиска", sender);
            return;
        }

        Search search = searchOpt.get();
        messageHandler.setEditingSearchId(chatId, search.getId());

        // Инициализируем tempCriteria с текущими данными поиска
        SearchCriteriaDto criteria = searchService.toDto(search);
        messageHandler.setTempCriteria(chatId, criteria);

        // Показываем текущие районы
        String currentDistricts;
        if (search.getDistricts() == null || search.getDistricts().isEmpty()) {
            currentDistricts = "Текущие районы: Все районы\n\n";
        } else {
            currentDistricts = String.format("Текущие районы: %s\n\n",
                    String.join(", ", search.getDistricts()));
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(currentDistricts + "Выберите новые районы:");
        message.setReplyMarkup(createEditDistrictSelection(search.getDistricts()));

        try {
            sender.execute(message);
            messageHandler.setState(chatId, ConversationState.EDITING_DISTRICTS);
        } catch (TelegramApiException e) {
            log.error("Error sending edit district selection to chatId: {}", chatId, e);
        }
    }

    /**
     * Обработка выбора комнат при редактировании
     */
    private void handleEditSetRooms(Long chatId, String data, AbsSender sender) {
        Long searchId = messageHandler.getEditingSearchId(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            return;
        }

        try {
            int numRooms = Integer.parseInt(data);
            Search search = searchService.findById(searchId);

            // Обновляем критерии
            SearchCriteriaDto criteria = searchService.toDto(search);
            criteria.setNumRooms(numRooms);
            searchService.updateCriteria(searchId, criteria);

            // Очищаем состояние
            messageHandler.resetConversation(chatId);

            sendMessage(chatId,
                    String.format("✅ Количество комнат обновлено: %d\n\n" +
                            "Изменения сохранены!", numRooms), sender);

        } catch (Exception e) {
            log.error("Error updating rooms for searchId: {}", searchId, e);
            sendMessage(chatId, "❌ Произошла ошибка при сохранении изменений", sender);
            messageHandler.resetConversation(chatId);
        }
    }

    /**
     * Переключение района при редактировании
     */
    private void handleEditToggleDistrict(Long chatId, String district, AbsSender sender) {
        Long searchId = messageHandler.getEditingSearchId(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            return;
        }

        try {
            // Получаем текущие критерии из tempCriteria или из поиска
            SearchCriteriaDto criteria = messageHandler.getTempCriteria(chatId);
            if (criteria == null) {
                Search search = searchService.findById(searchId);
                criteria = searchService.toDto(search);
            }

            if (criteria.getDistricts() == null) {
                criteria.setDistricts(new ArrayList<>());
            }

            // Переключаем район
            if (criteria.getDistricts().contains(district)) {
                criteria.getDistricts().remove(district);
            } else {
                criteria.getDistricts().add(district);
            }

            // Сохраняем обновленные критерии
            messageHandler.setTempCriteria(chatId, criteria);

            log.debug("District toggled: {}, current list: {}", district, criteria.getDistricts());

        } catch (Exception e) {
            log.error("Error toggling district", e);
        }
    }

    /**
     * Выбрать все районы при редактировании
     */
    private void handleEditDistrictsAll(Long chatId, AbsSender sender) {
        Long searchId = messageHandler.getEditingSearchId(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            return;
        }

        try {
            Search search = searchService.findById(searchId);
            SearchCriteriaDto criteria = searchService.toDto(search);
            criteria.setDistricts(new ArrayList<>());

            searchService.updateCriteria(searchId, criteria);

            messageHandler.resetConversation(chatId);

            sendMessage(chatId, "✅ Районы обновлены: Все районы\n\nИзменения сохранены!", sender);

        } catch (Exception e) {
            log.error("Error updating districts to all", e);
            sendMessage(chatId, "❌ Произошла ошибка при сохранении изменений", sender);
            messageHandler.resetConversation(chatId);
        }
    }

    /**
     * Завершить выбор районов при редактировании
     */
    private void handleEditDistrictsDone(Long chatId, AbsSender sender) {
        Long searchId = messageHandler.getEditingSearchId(chatId);
        if (searchId == null) {
            sendMessage(chatId, "❌ Произошла ошибка. Начните редактирование заново.", sender);
            return;
        }

        try {
            Search search = searchService.findById(searchId);
            SearchCriteriaDto newCriteria = messageHandler.getTempCriteria(chatId);

            if (newCriteria == null) {
                // Если tempCriteria нет, используем текущие районы
                sendMessage(chatId, "❌ Выберите хотя бы один район или нажмите \"Все районы\"", sender);
                return;
            }

            if (newCriteria.getDistricts() == null || newCriteria.getDistricts().isEmpty()) {
                sendMessage(chatId, "❌ Выберите хотя бы один район или нажмите \"Все районы\"", sender);
                return;
            }

            // Обновляем критерии
            SearchCriteriaDto criteria = searchService.toDto(search);
            criteria.setDistricts(newCriteria.getDistricts());
            searchService.updateCriteria(searchId, criteria);

            messageHandler.resetConversation(chatId);

            sendMessage(chatId,
                    String.format("✅ Районы обновлены: %s\n\nИзменения сохранены!",
                            String.join(", ", criteria.getDistricts())), sender);

        } catch (Exception e) {
            log.error("Error updating districts", e);
            sendMessage(chatId, "❌ Произошла ошибка при сохранении изменений", sender);
            messageHandler.resetConversation(chatId);
        }
    }

    /**
     * Отменить редактирование
     */
    private void handleCancelEdit(Long chatId, AbsSender sender) {
        messageHandler.resetConversation(chatId);
        sendMessage(chatId, "Редактирование отменено.", sender);
    }

    /**
     * Создать клавиатуру выбора комнат для редактирования
     */
    private InlineKeyboardMarkup createEditRoomSelection() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Ряд 1: 1 | 2 | 3
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(i + " комн.");
            btn.setCallbackData("EDIT_SET_ROOMS:" + i);
            row1.add(btn);
        }
        keyboard.add(row1);

        // Ряд 2: 4 | 5+
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn4 = new InlineKeyboardButton();
        btn4.setText("4 комн.");
        btn4.setCallbackData("EDIT_SET_ROOMS:4");
        row2.add(btn4);

        InlineKeyboardButton btn5 = new InlineKeyboardButton();
        btn5.setText("5+ комн.");
        btn5.setCallbackData("EDIT_SET_ROOMS:5");
        row2.add(btn5);
        keyboard.add(row2);

        // Ряд 3: Отмена
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("CANCEL_EDIT");
        row3.add(cancelBtn);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Создать клавиатуру выбора районов для редактирования
     */
    private InlineKeyboardMarkup createEditDistrictSelection(List<String> selectedDistricts) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        String[] districts = {
            "Ciutat Vella", "Ruzafa", "El Pla del Real",
            "Benimaclet", "Algirós", "Campanar",
            "L'Eixample", "Extramurs", "Poblats Marítims"
        };

        // По 2 кнопки в ряд
        for (int i = 0; i < districts.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int j = i; j < Math.min(i + 2, districts.length); j++) {
                String district = districts[j];
                InlineKeyboardButton btn = new InlineKeyboardButton();

                boolean isSelected = selectedDistricts != null && selectedDistricts.contains(district);
                btn.setText((isSelected ? "✅ " : "") + district);
                btn.setCallbackData("EDIT_TOGGLE_DISTRICT:" + district);

                row.add(btn);
            }
            keyboard.add(row);
        }

        // Последний ряд: Все районы | Готово | Отмена
        List<InlineKeyboardButton> lastRow = new ArrayList<>();

        InlineKeyboardButton allBtn = new InlineKeyboardButton();
        allBtn.setText("🌍 Все");
        allBtn.setCallbackData("EDIT_DISTRICTS_ALL");
        lastRow.add(allBtn);

        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Готово");
        doneBtn.setCallbackData("EDIT_DISTRICTS_DONE");
        lastRow.add(doneBtn);

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("CANCEL_EDIT");
        lastRow.add(cancelBtn);

        keyboard.add(lastRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Удалить поиск
     */
    private void handleDeleteSearch(Long chatId, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("⚠️ Вы уверены, что хотите удалить поиск?\n\n" +
                "Это действие нельзя отменить.");
        message.setReplyMarkup(keyboardFactory.createDeleteConfirmation());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation to chatId: {}", chatId, e);
        }
    }

    /**
     * Подтвердить удаление
     */
    private void handleConfirmDelete(Long chatId, AbsSender sender) {
        User user = userService.getUserByTelegramId(chatId);
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        if (searchOpt.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет активного поиска", sender);
            return;
        }

        searchService.deleteSearch(searchOpt.get().getId());
        sendMessage(chatId,
                "🗑 Поиск удален.\n\n" +
                "Вы можете создать новый поиск в любое время.", sender);
    }

    /**
     * Отменить удаление
     */
    private void handleCancelDelete(Long chatId, AbsSender sender) {
        sendMessage(chatId, "Удаление отменено.", sender);
    }

    /**
     * Вернуться в главное меню
     */
    private void handleBackToMain(Long chatId, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Главное меню:");
        message.setReplyMarkup(keyboardFactory.createMainMenu());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending main menu to chatId: {}", chatId, e);
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

    /**
     * Ответить на callback query
     */
    private void answerCallbackQuery(String callbackQueryId, AbsSender sender) {
        answerCallbackQuery(callbackQueryId, null, sender);
    }

    /**
     * Ответить на callback query с текстом
     */
    private void answerCallbackQuery(String callbackQueryId, String text, AbsSender sender) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        if (text != null) {
            answer.setText(text);
            answer.setShowAlert(true);
        }

        try {
            sender.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback query", e);
        }
    }
}
