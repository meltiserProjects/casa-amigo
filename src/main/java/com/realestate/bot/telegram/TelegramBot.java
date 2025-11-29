package com.realestate.bot.telegram;

import com.realestate.bot.config.BotConfig;
import com.realestate.bot.telegram.handler.CallbackQueryHandler;
import com.realestate.bot.telegram.handler.CommandHandler;
import com.realestate.bot.telegram.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Основной класс Telegram бота
 */
@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    public TelegramBot(
            BotConfig botConfig,
            CommandHandler commandHandler,
            MessageHandler messageHandler,
            CallbackQueryHandler callbackQueryHandler
    ) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.commandHandler = commandHandler;
        this.messageHandler = messageHandler;
        this.callbackQueryHandler = callbackQueryHandler;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            log.debug("Received update: {}", update.getUpdateId());

            // Обработка текстовых сообщений
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();

                // Если сообщение начинается с "/" - это команда
                if (messageText.startsWith("/")) {
                    log.debug("Processing command: {}", messageText);
                    commandHandler.handle(update, this);
                } else {
                    // Обработка обычных текстовых сообщений (ввод цен и т.д.)
                    log.debug("Processing text message: {}", messageText);
                    messageHandler.handle(update, this);
                }
            }
            // Обработка callback запросов (нажатия на кнопки)
            else if (update.hasCallbackQuery()) {
                log.debug("Processing callback query: {}", update.getCallbackQuery().getData());
                callbackQueryHandler.handle(update, this);
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", update.getUpdateId(), e);
        }
    }
}
