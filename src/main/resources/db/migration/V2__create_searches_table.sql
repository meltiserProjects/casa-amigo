-- Создание ENUM типа для статусов поиска
CREATE TYPE search_status AS ENUM ('ACTIVE', 'PAUSED', 'DELETED');

-- Создание таблицы поисковых запросов
CREATE TABLE searches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status search_status NOT NULL DEFAULT 'ACTIVE',

    -- Критерии поиска
    min_price INTEGER,
    max_price INTEGER,
    num_rooms INTEGER,
    districts TEXT[],  -- Массив районов Валенсии

    -- Метаданные
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_checked_at TIMESTAMP,

    -- ВАЖНО: Ограничение - только один активный поиск на пользователя
    CONSTRAINT unique_active_search_per_user
        EXCLUDE (user_id WITH =) WHERE (status = 'ACTIVE')
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_searches_user_id ON searches(user_id);
CREATE INDEX idx_searches_status ON searches(status);
CREATE INDEX idx_searches_user_status ON searches(user_id, status);

-- Комментарии к таблице
COMMENT ON TABLE searches IS 'Поисковые запросы пользователей для квартир в аренду';
COMMENT ON COLUMN searches.status IS 'Статус поиска: ACTIVE - активен, PAUSED - приостановлен, DELETED - удален';
COMMENT ON COLUMN searches.districts IS 'Массив названий районов Валенсии для поиска';
COMMENT ON COLUMN searches.last_checked_at IS 'Время последней проверки новых объявлений планировщиком';
COMMENT ON CONSTRAINT unique_active_search_per_user ON searches IS 'Гарантирует, что у пользователя может быть только один активный поиск';
