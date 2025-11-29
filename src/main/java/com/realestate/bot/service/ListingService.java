package com.realestate.bot.service;

import com.realestate.bot.model.dto.ListingDto;
import com.realestate.bot.model.dto.SearchCriteriaDto;
import com.realestate.bot.model.entity.Search;
import com.realestate.bot.model.entity.SentListing;
import com.realestate.bot.repository.SentListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для работы с объявлениями о недвижимости
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListingService {

    private final ApifyService apifyService;
    private final SentListingRepository sentListingRepository;

    /**
     * Получить новые объявления для поиска (не отправленные ранее)
     *
     * @param search поиск
     * @return список новых объявлений
     */
    public List<ListingDto> getNewListings(Search search) {
        log.info("Getting new listings for search: {}", search.getId());

        // Преобразуем Search в критерии
        SearchCriteriaDto criteria = SearchCriteriaDto.builder()
                .minPrice(search.getMinPrice())
                .maxPrice(search.getMaxPrice())
                .numRooms(search.getNumRooms())
                .districts(search.getDistricts())
                .build();

        // Ищем объявления через Apify
        List<ListingDto> allListings = apifyService.searchListings(criteria);

        // Фильтруем по районам если указаны
        List<ListingDto> filteredListings = apifyService.filterByDistricts(
                allListings,
                search.getDistricts()
        );

        // Получаем множество уже отправленных ID
        Set<String> sentIds = sentListingRepository.findIdealistaIdsBySearchId(search.getId());

        // Фильтруем новые объявления (те, которых нет в sentIds)
        List<ListingDto> newListings = filteredListings.stream()
                .filter(listing -> !sentIds.contains(listing.getIdealistaId()))
                .collect(Collectors.toList());

        log.info("Found {} total listings, {} new (not sent before)",
                filteredListings.size(), newListings.size());

        return newListings;
    }

    /**
     * Отметить объявление как отправленное
     *
     * @param search поиск
     * @param listing объявление
     */
    @Transactional
    public void markAsSent(Search search, ListingDto listing) {
        log.debug("Marking listing as sent: searchId={}, idealistaId={}",
                search.getId(), listing.getIdealistaId());

        // Проверяем, не было ли уже сохранено (на случай race condition)
        if (sentListingRepository.existsBySearchIdAndIdealistaId(
                search.getId(), listing.getIdealistaId())) {
            log.debug("Listing already marked as sent, skipping");
            return;
        }

        SentListing sentListing = SentListing.builder()
                .search(search)
                .idealistaId(listing.getIdealistaId())
                .idealistaUrl(listing.getIdealistaUrl())
                .price(listing.getPrice())
                .numRooms(listing.getNumRooms())
                .district(listing.getDistrict())
                .description(listing.getDescription())
                .photoUrls(listing.getPhotoUrls())
                .sentAt(LocalDateTime.now())
                .build();

        sentListingRepository.save(sentListing);
        log.debug("Listing marked as sent successfully");
    }

    /**
     * Отметить несколько объявлений как отправленные
     *
     * @param search поиск
     * @param listings список объявлений
     */
    @Transactional
    public void markAsSent(Search search, List<ListingDto> listings) {
        log.info("Marking {} listings as sent for search: {}", listings.size(), search.getId());

        for (ListingDto listing : listings) {
            try {
                markAsSent(search, listing);
            } catch (Exception e) {
                log.error("Error marking listing as sent: {}", listing.getIdealistaId(), e);
            }
        }
    }

    /**
     * Получить количество отправленных объявлений для поиска
     *
     * @param searchId ID поиска
     * @return количество объявлений
     */
    public long getCountSent(Long searchId) {
        return sentListingRepository.findBySearchId(searchId).size();
    }

    /**
     * Получить все отправленные объявления для поиска
     *
     * @param searchId ID поиска
     * @return список отправленных объявлений
     */
    public List<SentListing> getSentListings(Long searchId) {
        return sentListingRepository.findBySearchId(searchId);
    }
}
