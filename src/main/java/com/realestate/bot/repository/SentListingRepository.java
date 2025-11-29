package com.realestate.bot.repository;

import com.realestate.bot.model.entity.SentListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository для работы с отправленными объявлениями
 */
@Repository
public interface SentListingRepository extends JpaRepository<SentListing, Long> {

    /**
     * Найти все отправленные объявления для поиска
     *
     * @param searchId ID поиска
     * @return список отправленных объявлений
     */
    List<SentListing> findBySearchId(Long searchId);

    /**
     * Получить множество Idealista ID отправленных объявлений для поиска
     * Используется для быстрой проверки дубликатов
     *
     * @param searchId ID поиска
     * @return множество Idealista ID
     */
    @Query("SELECT sl.idealistaId FROM SentListing sl WHERE sl.search.id = :searchId")
    Set<String> findIdealistaIdsBySearchId(@Param("searchId") Long searchId);

    /**
     * Проверить, было ли объявление уже отправлено в рамках поиска
     *
     * @param searchId ID поиска
     * @param idealistaId ID объявления на Idealista
     * @return true если объявление уже было отправлено
     */
    boolean existsBySearchIdAndIdealistaId(Long searchId, String idealistaId);
}
