-- Создание таблицы пользователей Telegram
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска по telegram_id
CREATE INDEX idx_users_telegram_id ON users(telegram_id);

-- Комментарии к таблице
COMMENT ON TABLE users IS 'Пользователи Telegram бота';
COMMENT ON COLUMN users.telegram_id IS 'Уникальный ID пользователя в Telegram';
COMMENT ON COLUMN users.username IS 'Username в Telegram (@username)';
