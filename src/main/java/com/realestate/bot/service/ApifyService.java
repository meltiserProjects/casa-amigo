package com.realestate.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.bot.config.ApifyConfig;
import com.realestate.bot.exception.ApiException;
import com.realestate.bot.model.dto.ListingDto;
import com.realestate.bot.model.dto.SearchCriteriaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Сервис для работы с Apify Idealista Scraper API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApifyService {

    private final ApifyConfig apifyConfig;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    // Location ID для города Валенсия
    private static final String VALENCIA_LOCATION_ID = "0-EU-ES-46";

    /**
     * Искать квартиры по заданным критериям
     *
     * @param criteria критерии поиска
     * @return список найденных квартир
     */
    public List<ListingDto> searchListings(SearchCriteriaDto criteria) {
        log.info("Searching listings with criteria: minPrice={}, maxPrice={}, numRooms={}, districts={}",
                criteria.getMinPrice(), criteria.getMaxPrice(), criteria.getNumRooms(),
                criteria.getDistricts() != null ? criteria.getDistricts().size() : 0);

        try {
            // Строим тело запроса для Apify
            Map<String, Object> requestBody = buildApifyRequest(criteria);

            // Выполняем запрос к Apify API (sync endpoint)
            String url = String.format("https://api.apify.com/v2/acts/%s/run-sync?token=%s&timeout=%d",
                    apifyConfig.getActorId(),
                    apifyConfig.getApiKey(),
                    apifyConfig.getTimeout());

            log.debug("Calling Apify API: POST {}", url);

            String response = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(apifyConfig.getTimeout() + 10)) // Добавляем запас
                    .block();

            // Парсим ответ
            List<ListingDto> listings = parseResponse(response);

            log.info("Found {} listings from Apify", listings.size());
            return listings;

        } catch (WebClientResponseException e) {
            log.error("Apify API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ApiException("Ошибка при обращении к Apify API: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error searching listings", e);
            throw new ApiException("Ошибка поиска квартир: " + e.getMessage());
        }
    }

    /**
     * Построить тело запроса для Apify API
     */
    private Map<String, Object> buildApifyRequest(SearchCriteriaDto criteria) {
        Map<String, Object> request = new HashMap<>();

        // Базовые параметры
        request.put("operation", "rent");
        request.put("propertyType", "homes");
        request.put("locationId", VALENCIA_LOCATION_ID);

        // Цены (ВАЖНО: Apify требует строки, а не числа)
        if (criteria.getMinPrice() != null) {
            request.put("minPrice", String.valueOf(criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            request.put("maxPrice", String.valueOf(criteria.getMaxPrice()));
        }

        // Количество комнат (ВАЖНО: Apify требует массив строк)
        if (criteria.getNumRooms() != null) {
            request.put("bedrooms", new String[]{String.valueOf(criteria.getNumRooms())});
        }

        // Районы - Apify может не поддерживать фильтр по районам напрямую
        // Будем фильтровать на нашей стороне после получения результатов
        // (Или можно использовать locationId для каждого района, если есть mapping)

        // Максимальное количество результатов
        request.put("maxItems", 100);

        // Включить фотографии
        request.put("includeImages", true);

        log.debug("Built Apify request: {}", request);
        return request;
    }

    /**
     * Распарсить ответ от Apify и преобразовать в список ListingDto
     */
    private List<ListingDto> parseResponse(String response) throws Exception {
        List<ListingDto> listings = new ArrayList<>();

        // Парсим JSON ответ
        JsonNode root = objectMapper.readTree(response);

        // Проверяем статус выполнения
        JsonNode statusNode = root.path("status");
        if (!statusNode.isMissingNode()) {
            String status = statusNode.asText();
            if (!"SUCCEEDED".equals(status)) {
                log.warn("Apify run status is not SUCCEEDED: {}", status);
                return listings;
            }
        }

        // Извлекаем элементы из defaultDatasetId
        JsonNode itemsNode = root.path("defaultDatasetId");
        if (itemsNode.isMissingNode()) {
            log.warn("No defaultDatasetId in response");
            return listings;
        }

        // Если Apify возвращает items напрямую в sync endpoint
        JsonNode items = root.path("items");
        if (items.isMissingNode()) {
            // Пробуем другую структуру
            items = root;
            if (items.isArray()) {
                items = root;
            } else {
                log.warn("Cannot find items array in response");
                return listings;
            }
        }

        // Преобразуем каждый элемент в ListingDto
        if (items.isArray()) {
            for (JsonNode item : items) {
                try {
                    ListingDto listing = mapToListingDto(item);
                    if (listing != null) {
                        listings.add(listing);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse listing item", e);
                }
            }
        }

        return listings;
    }

    /**
     * Преобразовать JSON элемент в ListingDto
     */
    private ListingDto mapToListingDto(JsonNode item) {
        try {
            // Извлекаем данные из JSON (структура зависит от Apify Idealista Scraper)
            String url = item.path("url").asText();
            if (url == null || url.isEmpty()) {
                return null;
            }

            // ID квартиры (извлекаем из URL или используем propertyCode)
            String propertyId = item.path("propertyCode").asText();
            if (propertyId == null || propertyId.isEmpty()) {
                // Пробуем извлечь из URL
                propertyId = extractIdFromUrl(url);
            }

            // Цена
            Integer price = null;
            JsonNode priceNode = item.path("price");
            if (!priceNode.isMissingNode()) {
                price = priceNode.asInt();
            }

            // Количество комнат
            Integer numRooms = null;
            JsonNode roomsNode = item.path("rooms");
            if (!roomsNode.isMissingNode()) {
                numRooms = roomsNode.asInt();
            }

            // Район
            String district = item.path("district").asText(null);
            if (district == null || district.isEmpty()) {
                district = item.path("neighborhood").asText(null);
            }

            // Описание
            String description = item.path("description").asText(null);

            // Фотографии (берем первые 3)
            List<String> photoUrls = new ArrayList<>();
            JsonNode imagesNode = item.path("images");
            if (imagesNode.isArray()) {
                int count = 0;
                for (JsonNode imageNode : imagesNode) {
                    if (count >= 3) break;
                    String imageUrl = imageNode.asText();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        photoUrls.add(imageUrl);
                        count++;
                    }
                }
            }

            // Создаем DTO
            return ListingDto.builder()
                    .idealistaId(propertyId)
                    .idealistaUrl(url)
                    .price(price)
                    .numRooms(numRooms)
                    .district(district)
                    .description(description)
                    .photoUrls(photoUrls)
                    .build();

        } catch (Exception e) {
            log.warn("Error mapping item to ListingDto", e);
            return null;
        }
    }

    /**
     * Извлечь ID квартиры из URL
     */
    private String extractIdFromUrl(String url) {
        if (url == null) return null;

        // URL Idealista обычно содержит ID в конце, например:
        // https://www.idealista.com/inmueble/98765432/
        try {
            String[] parts = url.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i];
                if (!part.isEmpty() && part.matches("\\d+")) {
                    return part;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract ID from URL: {}", url);
        }

        // Если не получилось, используем весь URL как ID
        return String.valueOf(url.hashCode());
    }

    /**
     * Фильтровать результаты по районам
     *
     * @param listings список квартир
     * @param districts список желаемых районов (null или пустой = все районы)
     * @return отфильтрованный список
     */
    public List<ListingDto> filterByDistricts(List<ListingDto> listings, List<String> districts) {
        // Если районы не указаны, возвращаем все
        if (districts == null || districts.isEmpty()) {
            return listings;
        }

        // Фильтруем по районам (игнорируем регистр)
        List<String> districtsLower = districts.stream()
                .map(String::toLowerCase)
                .toList();

        return listings.stream()
                .filter(listing -> {
                    if (listing.getDistrict() == null) {
                        return false;
                    }
                    String listingDistrict = listing.getDistrict().toLowerCase();
                    return districtsLower.stream()
                            .anyMatch(listingDistrict::contains);
                })
                .toList();
    }
}
