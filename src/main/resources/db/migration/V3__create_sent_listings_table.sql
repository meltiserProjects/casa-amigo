-- Создание таблицы отправленных объявлений
CREATE TABLE sent_listings (
    id BIGSERIAL PRIMARY KEY,
    search_id BIGINT NOT NULL REFERENCES searches(id) ON DELETE CASCADE,

    -- Информация об объявлении из Idealista
    idealista_id VARCHAR(255) NOT NULL,
    idealista_url TEXT NOT NULL,
    price INTEGER NOT NULL,
    num_rooms INTEGER,
    district VARCHAR(255),
    description TEXT,
    photo_urls TEXT[],  -- Массив URL фотографий

    -- Метаданные
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ВАЖНО: Предотвращение отправки дубликатов одного объявления в рамках поиска
    CONSTRAINT unique_listing_per_search UNIQUE (search_id, idealista_id)
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_sent_listings_search_id ON sent_listings(search_id);
CREATE INDEX idx_sent_listings_idealista_id ON sent_listings(idealista_id);
CREATE INDEX idx_sent_listings_sent_at ON sent_listings(sent_at);

-- Комментарии к таблице
COMMENT ON TABLE sent_listings IS 'Отправленные пользователям объявления о квартирах (для дедупликации)';
COMMENT ON COLUMN sent_listings.idealista_id IS 'Уникальный ID объявления на Idealista (propertyCode)';
COMMENT ON COLUMN sent_listings.idealista_url IS 'Прямая ссылка на объявление на сайте Idealista';
COMMENT ON COLUMN sent_listings.photo_urls IS 'Массив URL-ов первых 3 фотографий квартиры';
COMMENT ON CONSTRAINT unique_listing_per_search ON sent_listings IS 'Гарантирует отправку каждого объявления пользователю только один раз в рамках поиска';
