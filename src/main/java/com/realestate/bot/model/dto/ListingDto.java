package com.realestate.bot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для объявления о квартире из Idealista
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {

    /**
     * ID объявления на Idealista (propertyCode)
     */
    private String idealistaId;

    /**
     * URL объявления на Idealista
     */
    private String idealistaUrl;

    /**
     * Цена аренды (EUR)
     */
    private Integer price;

    /**
     * Количество комнат
     */
    private Integer numRooms;

    /**
     * Район
     */
    private String district;

    /**
     * Описание квартиры
     */
    private String description;

    /**
     * URL фотографий (первые 3)
     */
    private List<String> photoUrls;

    /**
     * Форматирование в текст для отправки в Telegram
     */
    public String toTelegramMessage() {
        StringBuilder message = new StringBuilder();
        message.append("🏠 Новая квартира найдена!\n\n");
        message.append(String.format("💰 Цена: %,d EUR\n", price));

        if (numRooms != null) {
            message.append(String.format("🛏 Комнат: %d\n", numRooms));
        }

        if (district != null && !district.isEmpty()) {
            message.append(String.format("📍 Район: %s\n", district));
        }

        if (description != null && !description.isEmpty()) {
            message.append("\n📝 Описание:\n");
            // Обрезаем описание до 300 символов
            String truncatedDesc = description.length() > 300
                    ? description.substring(0, 297) + "..."
                    : description;
            message.append(truncatedDesc).append("\n");
        }

        message.append(String.format("\n🔗 Ссылка: %s\n", idealistaUrl));

        if (photoUrls != null && !photoUrls.isEmpty()) {
            message.append("\n📸 Фотографии:\n");
            for (int i = 0; i < Math.min(3, photoUrls.size()); i++) {
                message.append(photoUrls.get(i)).append("\n");
            }
        }

        return message.toString();
    }
}
