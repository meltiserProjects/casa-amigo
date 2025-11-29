package com.realestate.bot.exception;

/**
 * Исключение, возникающее при попытке создать больше одного активного поиска
 */
public class SearchLimitException extends RuntimeException {

    public SearchLimitException(String message) {
        super(message);
    }

    public SearchLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
