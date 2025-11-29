package com.realestate.bot.repository;

import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.enums.SearchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с поисковыми запросами
 */
@Repository
public interface SearchRepository extends JpaRepository<Search, Long> {

    /**
     * Найти поиск пользователя по статусу
     *
     * @param userId ID пользователя
     * @param status статус поиска
     * @return Optional с поиском, если найден
     */
    Optional<Search> findByUserIdAndStatus(Long userId, SearchStatus status);

    /**
     * Найти все поиски пользователя
     *
     * @param userId ID пользователя
     * @return список всех поисков пользователя
     */
    List<Search> findByUserId(Long userId);

    /**
     * Найти все поиски по статусу
     *
     * @param status статус поиска
     * @return список поисков с указанным статусом
     */
    List<Search> findByStatus(SearchStatus status);

    /**
     * Проверить существование активного поиска у пользователя
     *
     * @param userId ID пользователя
     * @param status статус поиска
     * @return true если у пользователя есть поиск с указанным статусом
     */
    boolean existsByUserIdAndStatus(Long userId, SearchStatus status);
}
