-- Изменение типа status с ENUM на VARCHAR для совместимости с Hibernate 6.5+

-- Шаг 1: Удаляем constraint unique_active_search_per_user (он использует ENUM тип)
ALTER TABLE searches DROP CONSTRAINT IF EXISTS unique_active_search_per_user;

-- Шаг 2: Изменяем тип колонки с ENUM на VARCHAR
ALTER TABLE searches ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Шаг 3: Удаляем старый ENUM тип (CASCADE для удаления зависимых объектов типа индексов)
DROP TYPE IF EXISTS search_status CASCADE;

-- Шаг 4: Добавляем CHECK constraint для валидации значений
ALTER TABLE searches ADD CONSTRAINT check_search_status
    CHECK (status IN ('ACTIVE', 'PAUSED', 'DELETED'));

-- Шаг 5: Восстанавливаем constraint для одного активного поиска на пользователя
ALTER TABLE searches ADD CONSTRAINT unique_active_search_per_user
    EXCLUDE (user_id WITH =) WHERE (status = 'ACTIVE');

-- Шаг 6: Восстанавливаем индексы (которые были удалены CASCADE)
CREATE INDEX IF NOT EXISTS idx_searches_status ON searches(status);
CREATE INDEX IF NOT EXISTS idx_searches_user_status ON searches(user_id, status);

-- Обновляем комментарий
COMMENT ON COLUMN searches.status IS 'Статус поиска: ACTIVE - активен, PAUSED - приостановлен, DELETED - удален (VARCHAR с CHECK constraint)';
