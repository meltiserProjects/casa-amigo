package com.realestate.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA сущность для отправленных объявлений (для дедупликации)
 */
@Entity
@Table(name = "sent_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_id", nullable = false)
    private Search search;

    // Информация об объявлении из Idealista
    @Column(name = "idealista_id", nullable = false)
    private String idealistaId;

    @Column(name = "idealista_url", nullable = false, columnDefinition = "TEXT")
    private String idealistaUrl;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "num_rooms")
    private Integer numRooms;

    @Column(name = "district")
    private String district;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_urls", columnDefinition = "TEXT[]")
    private List<String> photoUrls;

    // Метаданные
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
