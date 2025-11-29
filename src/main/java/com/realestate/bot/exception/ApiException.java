package com.realestate.bot.exception;

/**
 * Исключение для ошибок при работе с внешними API (Apify, Telegram)
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
