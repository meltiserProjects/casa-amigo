package com.realestate.bot.repository;

import com.realestate.bot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с пользователями Telegram
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по Telegram ID
     *
     * @param telegramId уникальный ID пользователя в Telegram
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Проверить существование пользователя по Telegram ID
     *
     * @param telegramId уникальный ID пользователя в Telegram
     * @return true если пользователь существует
     */
    boolean existsByTelegramId(Long telegramId);
}
