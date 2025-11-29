package com.realestate.bot.service;

import com.realestate.bot.model.entity.User;
import com.realestate.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервис для управления пользователями Telegram
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Найти или создать пользователя по данным из Telegram
     *
     * @param telegramUser объект пользователя из Telegram API
     * @return сущность пользователя из БД
     */
    @Transactional
    public User findOrCreateUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return userRepository.findByTelegramId(telegramUser.getId())
                .orElseGet(() -> createUser(telegramUser));
    }

    /**
     * Создать нового пользователя
     *
     * @param telegramUser объект пользователя из Telegram API
     * @return созданная сущность пользователя
     */
    private User createUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        log.info("Creating new user: telegramId={}, username={}, firstName={}",
                telegramUser.getId(),
                telegramUser.getUserName(),
                telegramUser.getFirstName());

        User user = User.builder()
                .telegramId(telegramUser.getId())
                .username(telegramUser.getUserName())
                .firstName(telegramUser.getFirstName())
                .lastName(telegramUser.getLastName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: id={}, telegramId={}", savedUser.getId(), savedUser.getTelegramId());

        return savedUser;
    }

    /**
     * Получить пользователя по ID
     *
     * @param id ID пользователя в БД
     * @return пользователь
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    /**
     * Получить пользователя по Telegram ID
     *
     * @param telegramId Telegram ID пользователя
     * @return пользователь
     */
    public User getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with telegramId: " + telegramId));
    }
}
