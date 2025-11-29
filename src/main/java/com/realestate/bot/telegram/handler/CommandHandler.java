package com.realestate.bot.telegram.handler;

import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.entity.User;
import com.realestate.bot.model.enums.SearchStatus;
import com.realestate.bot.service.SearchService;
import com.realestate.bot.service.UserService;
import com.realestate.bot.telegram.keyboard.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

/**
 * Обработчик команд Telegram бота
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommandHandler {

    private final UserService userService;
    private final SearchService searchService;
    private final KeyboardFactory keyboardFactory;

    /**
     * Обработать команду
     */
    public void handle(Update update, AbsSender sender) {
        Message message = update.getMessage();
        String command = message.getText().split(" ")[0];
        Long chatId = message.getChatId();

        log.debug("Processing command: {} from chatId: {}", command, chatId);

        switch (command) {
            case "/start" -> handleStart(update, sender);
            case "/help" -> handleHelp(update, sender);
            case "/mysearch" -> handleMySearch(update, sender);
            default -> handleUnknown(update, sender);
        }
    }

    /**
     * Обработка команды /start
     */
    private void handleStart(Update update, AbsSender sender) {
        org.telegram.telegrambots.meta.api.objects.User telegramUser = update.getMessage().getFrom();
        Long chatId = update.getMessage().getChatId();

        // Регистрируем пользователя или находим существующего
        User user = userService.findOrCreateUser(telegramUser);

        log.info("User {} started bot: telegramId={}, userId={}",
                user.getFirstName(), user.getTelegramId(), user.getId());

        // Формируем приветственное сообщение
        String welcomeMessage = String.format(
                "Добро пожаловать в ValenciaRentBot! 🏠\n\n" +
                "Привет, %s! Я помогу вам найти квартиру в аренду в Валенсии.\n\n" +
                "Что я умею:\n" +
                "✅ Искать квартиры по вашим критериям (цена, комнаты, районы)\n" +
                "✅ Отправлять уведомления о новых объявлениях (каждые 15 минут)\n" +
                "✅ Не дублировать уже отправленные предложения\n\n" +
                "Выберите действие:",
                user.getFirstName() != null ? user.getFirstName() : "друг"
        );

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(welcomeMessage);
        sendMessage.setReplyMarkup(keyboardFactory.createMainMenu());

        try {
            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending start message to chatId: {}", chatId, e);
        }
    }

    /**
     * Обработка команды /help
     */
    private void handleHelp(Update update, AbsSender sender) {
        Long chatId = update.getMessage().getChatId();

        String helpMessage = "❓ Помощь\n\n" +
                "Доступные команды:\n" +
                "/start - Начать работу с ботом\n" +
                "/mysearch - Посмотреть мой активный поиск\n" +
                "/help - Показать эту справку\n\n" +
                "Как пользоваться:\n" +
                "1. Создайте поиск с вашими критериями\n" +
                "2. Получите текущие предложения сразу\n" +
                "3. Бот будет присылать новые каждые 15 минут\n" +
                "4. Управляйте поиском: приостановить, редактировать, удалить\n\n" +
                "Ограничения:\n" +
                "• Только один активный поиск на пользователя\n" +
                "• Только город Валенсия\n" +
                "• Проверка новых: каждые 15 минут";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(helpMessage);
        sendMessage.setReplyMarkup(keyboardFactory.createMainMenu());

        try {
            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending help message to chatId: {}", chatId, e);
        }
    }

    /**
     * Обработка команды /mysearch
     */
    private void handleMySearch(Update update, AbsSender sender) {
        org.telegram.telegrambots.meta.api.objects.User telegramUser = update.getMessage().getFrom();
        Long chatId = update.getMessage().getChatId();

        // Находим пользователя
        User user = userService.findOrCreateUser(telegramUser);

        // Ищем активный поиск
        Optional<Search> searchOpt = searchService.getActiveSearch(user.getId());

        String messageText;
        if (searchOpt.isEmpty()) {
            messageText = "У вас пока нет активного поиска.\n\n" +
                    "Создайте поиск, чтобы получать уведомления о новых квартирах!";
        } else {
            Search search = searchOpt.get();
            messageText = searchService.formatSearchInfo(search);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(messageText);

        // Если есть активный поиск, показываем клавиатуру управления
        if (searchOpt.isPresent()) {
            Search search = searchOpt.get();
            sendMessage.setReplyMarkup(keyboardFactory.createSearchManagement(
                    search.getStatus() == SearchStatus.ACTIVE));
        } else {
            sendMessage.setReplyMarkup(keyboardFactory.createMainMenu());
        }

        try {
            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending mysearch message to chatId: {}", chatId, e);
        }
    }

    /**
     * Обработка неизвестной команды
     */
    private void handleUnknown(Update update, AbsSender sender) {
        Long chatId = update.getMessage().getChatId();

        String message = "Неизвестная команда. Используйте /help для списка доступных команд.";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);

        try {
            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending unknown command message to chatId: {}", chatId, e);
        }
    }
}
