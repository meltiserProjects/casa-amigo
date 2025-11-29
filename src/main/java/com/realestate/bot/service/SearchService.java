package com.realestate.bot.service;

import com.realestate.bot.exception.SearchLimitException;
import com.realestate.bot.model.dto.SearchCriteriaDto;
import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.entity.User;
import com.realestate.bot.model.enums.SearchStatus;
import com.realestate.bot.repository.SearchRepository;
import com.realestate.bot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления поисковыми запросами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchRepository searchRepository;
    private final UserRepository userRepository;

    /**
     * Создать новый поиск
     *
     * @param userId   ID пользователя
     * @param criteria критерии поиска
     * @return созданный поиск
     * @throws SearchLimitException если у пользователя уже есть активный поиск
     */
    @Transactional
    public Search createSearch(Long userId, SearchCriteriaDto criteria) {
        log.info("Creating search for userId: {}", userId);

        // Проверяем наличие активного поиска
        Optional<Search> existingActive = searchRepository
                .findByUserIdAndStatus(userId, SearchStatus.ACTIVE);

        if (existingActive.isPresent()) {
            log.warn("User {} already has an active search", userId);
            throw new SearchLimitException(
                    "У вас уже есть активный поиск. Приостановите или удалите его перед созданием нового."
            );
        }

        // Валидация критериев
        if (!criteria.isValid()) {
            throw new IllegalArgumentException("Некорректные критерии поиска");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Search search = Search.builder()
                .user(user)
                .status(SearchStatus.ACTIVE)
                .minPrice(criteria.getMinPrice())
                .maxPrice(criteria.getMaxPrice())
                .numRooms(criteria.getNumRooms())
                .districts(criteria.getDistricts())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Search savedSearch = searchRepository.save(search);
        log.info("Search created successfully: id={}, userId={}", savedSearch.getId(), userId);

        return savedSearch;
    }

    /**
     * Получить активный поиск пользователя
     *
     * @param userId ID пользователя
     * @return активный поиск или пустой Optional
     */
    public Optional<Search> getActiveSearch(Long userId) {
        return searchRepository.findByUserIdAndStatus(userId, SearchStatus.ACTIVE);
    }

    /**
     * Приостановить поиск
     *
     * @param searchId ID поиска
     */
    @Transactional
    public void pauseSearch(Long searchId) {
        log.info("Pausing search: {}", searchId);

        Search search = findById(searchId);
        search.setStatus(SearchStatus.PAUSED);
        search.setUpdatedAt(LocalDateTime.now());
        searchRepository.save(search);

        log.info("Search paused: {}", searchId);
    }

    /**
     * Возобновить поиск
     *
     * @param searchId ID поиска
     * @throws SearchLimitException если у пользователя уже есть другой активный поиск
     */
    @Transactional
    public void resumeSearch(Long searchId) {
        log.info("Resuming search: {}", searchId);

        Search search = findById(searchId);

        // Проверяем что у пользователя нет другого активного поиска
        Optional<Search> existingActive = searchRepository
                .findByUserIdAndStatus(search.getUser().getId(), SearchStatus.ACTIVE);

        if (existingActive.isPresent() && !existingActive.get().getId().equals(searchId)) {
            log.warn("User {} already has another active search", search.getUser().getId());
            throw new SearchLimitException("У вас уже есть другой активный поиск");
        }

        search.setStatus(SearchStatus.ACTIVE);
        search.setUpdatedAt(LocalDateTime.now());
        searchRepository.save(search);

        log.info("Search resumed: {}", searchId);
    }

    /**
     * Обновить критерии поиска
     *
     * @param searchId ID поиска
     * @param criteria новые критерии
     */
    @Transactional
    public void updateCriteria(Long searchId, SearchCriteriaDto criteria) {
        log.info("Updating search criteria: {}", searchId);

        if (!criteria.isValid()) {
            throw new IllegalArgumentException("Некорректные критерии поиска");
        }

        Search search = findById(searchId);
        search.setMinPrice(criteria.getMinPrice());
        search.setMaxPrice(criteria.getMaxPrice());
        search.setNumRooms(criteria.getNumRooms());
        search.setDistricts(criteria.getDistricts());
        search.setUpdatedAt(LocalDateTime.now());
        searchRepository.save(search);

        log.info("Search criteria updated: {}", searchId);
    }

    /**
     * Удалить поиск
     *
     * @param searchId ID поиска
     */
    @Transactional
    public void deleteSearch(Long searchId) {
        log.info("Deleting search: {}", searchId);

        Search search = findById(searchId);
        search.setStatus(SearchStatus.DELETED);
        search.setUpdatedAt(LocalDateTime.now());
        searchRepository.save(search);

        log.info("Search deleted: {}", searchId);
    }

    /**
     * Получить все активные поиски (для планировщика)
     *
     * @return список активных поисков
     */
    public List<Search> findAllActive() {
        return searchRepository.findByStatus(SearchStatus.ACTIVE);
    }

    /**
     * Обновить время последней проверки
     *
     * @param searchId ID поиска
     */
    @Transactional
    public void updateLastChecked(Long searchId) {
        Search search = findById(searchId);
        search.setLastCheckedAt(LocalDateTime.now());
        searchRepository.save(search);
    }

    /**
     * Найти поиск по ID
     *
     * @param searchId ID поиска
     * @return поиск
     * @throws EntityNotFoundException если поиск не найден
     */
    public Search findById(Long searchId) {
        return searchRepository.findById(searchId)
                .orElseThrow(() -> new EntityNotFoundException("Search not found with id: " + searchId));
    }

    /**
     * Преобразовать Search в SearchCriteriaDto
     *
     * @param search поиск
     * @return DTO с критериями
     */
    public SearchCriteriaDto toDto(Search search) {
        return SearchCriteriaDto.builder()
                .minPrice(search.getMinPrice())
                .maxPrice(search.getMaxPrice())
                .numRooms(search.getNumRooms())
                .districts(search.getDistricts())
                .build();
    }

    /**
     * Форматировать поиск для отображения в Telegram
     *
     * @param search поиск
     * @return отформатированный текст
     */
    public String formatSearchInfo(Search search) {
        StringBuilder info = new StringBuilder();
        info.append("📋 Ваш активный поиск:\n\n");

        if (search.getMinPrice() != null || search.getMaxPrice() != null) {
            info.append("💰 Цена: ");
            if (search.getMinPrice() != null && search.getMaxPrice() != null) {
                info.append(String.format("%,d - %,d EUR\n", search.getMinPrice(), search.getMaxPrice()));
            } else if (search.getMinPrice() != null) {
                info.append(String.format("от %,d EUR\n", search.getMinPrice()));
            } else {
                info.append(String.format("до %,d EUR\n", search.getMaxPrice()));
            }
        }

        if (search.getNumRooms() != null) {
            info.append(String.format("🛏 Комнат: %d\n", search.getNumRooms()));
        }

        if (search.getDistricts() != null && !search.getDistricts().isEmpty()) {
            info.append("📍 Районы: ");
            info.append(String.join(", ", search.getDistricts()));
            info.append("\n");
        }

        info.append("\n");

        if (search.getStatus() == SearchStatus.ACTIVE) {
            info.append("Статус: Активен ✅\n");
        } else {
            info.append("Статус: Приостановлен ⏸\n");
        }

        if (search.getLastCheckedAt() != null) {
            info.append(String.format("Последняя проверка: %s\n",
                    formatDateTime(search.getLastCheckedAt())));
        }

        return info.toString();
    }

    /**
     * Форматировать дату и время для отображения
     */
    private String formatDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutesAgo < 1) {
            return "только что";
        } else if (minutesAgo < 60) {
            return minutesAgo + " мин. назад";
        } else if (minutesAgo < 24 * 60) {
            return (minutesAgo / 60) + " ч. назад";
        } else {
            return (minutesAgo / (24 * 60)) + " дн. назад";
        }
    }
}
