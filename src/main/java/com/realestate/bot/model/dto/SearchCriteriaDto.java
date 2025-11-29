package com.realestate.bot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для критериев поиска квартир
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDto {

    /**
     * Минимальная цена (EUR)
     */
    private Integer minPrice;

    /**
     * Максимальная цена (EUR)
     */
    private Integer maxPrice;

    /**
     * Количество комнат
     */
    private Integer numRooms;

    /**
     * Список районов Валенсии
     */
    private List<String> districts;

    /**
     * Валидация критериев
     */
    public boolean isValid() {
        // Проверяем что хотя бы один критерий задан
        if (minPrice == null && maxPrice == null && numRooms == null &&
            (districts == null || districts.isEmpty())) {
            return false;
        }

        // Если заданы обе цены, минимальная должна быть меньше максимальной
        if (minPrice != null && maxPrice != null && minPrice >= maxPrice) {
            return false;
        }

        // Цены должны быть положительными
        if (minPrice != null && minPrice < 0) {
            return false;
        }
        if (maxPrice != null && maxPrice < 0) {
            return false;
        }

        // Количество комнат должно быть от 1 до 5+
        if (numRooms != null && (numRooms < 1 || numRooms > 5)) {
            return false;
        }

        return true;
    }
}
