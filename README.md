# ValenciaRentBot - Telegram-бот для поиска аренды в Валенсии

Автоматический поиск квартир в аренду на Idealista с уведомлениями в Telegram.

https://apify.com/igolaizola/idealista-scraper/api/openapi

## Возможности

- Автоматический поиск квартир по заданным критериям (цена, комнаты, районы)
- Мгновенные уведомления о новых объявлениях (проверка каждые 15 минут)
- Управление поиском: создание, приостановка, редактирование, удаление
- Дедупликация объявлений (не отправляет повторно)
- Интеграция с Apify Idealista Scraper API

## Технологии

- Java 21
- Spring Boot 3.2.0
- Gradle 8.14
- PostgreSQL 16
- Telegram Bots API 6.9.7
- Apify API
- Flyway (миграции БД)
- Docker & Docker Compose

## Предварительные требования

### Вариант А: С Docker (рекомендуется)
1. Docker и Docker Compose
2. Telegram Bot Token (получить у [@BotFather](https://t.me/botfather))
3. Apify API Key (зарегистрироваться на [apify.com](https://apify.com))

### Вариант Б: Локальная установка
1. **Java 21** (ВАЖНО: Java 24 не поддерживается из-за несовместимости Hibernate с JAXB)
2. PostgreSQL 16 или выше
3. Telegram Bot Token (получить у [@BotFather](https://t.me/botfather))
4. Apify API Key (зарегистрироваться на [apify.com](https://apify.com))

## Установка и запуск

### Вариант А: Гибридный запуск (PostgreSQL в Docker, бот локально) - рекомендуется

Этот вариант позволяет запускать PostgreSQL в Docker, а бота - локально для удобной разработки.

#### 1. Настроить переменные окружения

Скопируйте файл `.env.example` в `.env`:

```bash
cp .env.example .env
```

Отредактируйте `.env` и укажите ваши данные.

#### 2. Запустить PostgreSQL в Docker

```bash
docker-compose up -d postgres

# Проверить статус
docker-compose ps
```

#### 3. Запустить бота локально

```bash
./gradlew bootRun
```

#### 4. Остановка

```bash
# Остановить бота: Ctrl+C в терминале

# Остановить PostgreSQL
docker-compose down
```

---

### Вариант Б: Запуск с Docker Compose (полностью в Docker)

#### 1. Настроить переменные окружения

Скопируйте файл `.env.example` в `.env`:

```bash
cp .env.example .env
```

Отредактируйте `.env` и укажите ваши данные:

```env
# Telegram Bot
BOT_TOKEN=your_bot_token_here
BOT_USERNAME=ValenciaRentBot

# Apify API
APIFY_API_KEY=your_apify_api_key_here

# Database (можно оставить по умолчанию)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=real_estate_bot
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

#### 2. Запустить приложение

```bash
# Собрать и запустить все сервисы (PostgreSQL + Bot)
docker-compose up -d

# Просмотр логов бота
docker-compose logs -f bot

# Просмотр логов базы данных
docker-compose logs -f postgres

# Остановить все сервисы
docker-compose down

# Остановить с удалением данных БД
docker-compose down -v
```

#### 3. Проверить работу

Откройте Telegram и найдите вашего бота по username. Отправьте команду `/start`.

---

### Вариант В: Локальная установка (без Docker)

#### 1. Создать базу данных PostgreSQL

```bash
# Войти в PostgreSQL
psql -U postgres

# Создать базу данных
CREATE DATABASE real_estate_bot;

# Выйти
\q
```

#### 2. Настроить переменные окружения

Скопируйте `.env.example` в `.env` и укажите ваши данные:

```properties
# Telegram Bot
BOT_TOKEN=your_bot_token_here
BOT_USERNAME=ValenciaRentBot

# Apify API
APIFY_API_KEY=your_apify_api_key_here

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=real_estate_bot
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
```

#### 3. Запустить приложение

**Через IntelliJ IDEA:**

1. Открыть проект в IntelliJ IDEA
2. Дождаться импорта Gradle зависимостей
3. Запустить `RealEstateBotApplication.java`

**Через командную строку:**

```bash
# На Windows
gradlew.bat bootRun

# На macOS/Linux
./gradlew bootRun
```

#### 4. Проверить работу

Откройте Telegram и найдите вашего бота по username. Отправьте команду `/start`.

## Структура проекта

```
real_estate_bot/
├── src/main/java/com/realestate/bot/
│   ├── RealEstateBotApplication.java   # Main класс
│   ├── config/                          # Конфигурация
│   ├── model/                           # Entity, DTO, Enums
│   ├── repository/                      # Spring Data JPA
│   ├── service/                         # Бизнес-логика
│   ├── telegram/                        # Telegram bot handlers
│   └── exception/                       # Обработка исключений
├── src/main/resources/
│   ├── application.yml                  # Конфигурация Spring
│   └── db/migration/                    # Flyway миграции
├── build.gradle                         # Зависимости Gradle
└── .env                                 # Переменные окружения
```

## Использование бота

### Создание поиска

1. `/start` - начать работу с ботом
2. Нажать "Создать поиск"
3. Ввести минимальную цену (EUR)
4. Ввести максимальную цену (EUR)
5. Выбрать количество комнат
6. Выбрать один или несколько районов
7. Нажать "Готово"

Бот сразу отправит все актуальные предложения и будет проверять новые каждые 15 минут.

### Управление поиском

- `/mysearch` - посмотреть активный поиск
- "Приостановить" - остановить уведомления (поиск сохранится)
- "Возобновить" - продолжить получать уведомления
- "Редактировать" - изменить критерии поиска
- "Удалить" - полностью удалить поиск

## Ограничения

- Один активный поиск на пользователя
- Только город: Валенсия, Испания
- Проверка новых объявлений: каждые 15 минут
- Отправка: первые 3 фотографии объявления

## База данных

### Таблицы

1. **users** - пользователи Telegram
2. **searches** - поисковые запросы с критериями
3. **sent_listings** - отправленные объявления (для дедупликации)

### Миграции

Flyway автоматически применяет миграции при запуске:
- V1: создание таблицы users
- V2: создание таблицы searches (с constraint для одного активного поиска)
- V3: создание таблицы sent_listings (с constraint для дедупликации)

## Docker команды

### Основные команды

```bash
# Запуск
docker-compose up -d                    # Запустить в фоне
docker-compose up                       # Запустить с выводом логов

# Остановка
docker-compose stop                     # Остановить (данные сохранятся)
docker-compose down                     # Остановить и удалить контейнеры
docker-compose down -v                  # Остановить и удалить volumes (БД)

# Логи
docker-compose logs -f                  # Все логи
docker-compose logs -f bot             # Логи бота
docker-compose logs -f postgres        # Логи БД

# Перезапуск
docker-compose restart                  # Перезапустить все сервисы
docker-compose restart bot             # Перезапустить только бота

# Пересборка
docker-compose build                    # Пересобрать образы
docker-compose up -d --build           # Пересобрать и запустить
```

### Доступ к базе данных

```bash
# Подключиться к PostgreSQL в контейнере
docker-compose exec postgres psql -U postgres -d real_estate_bot

# Выполнить SQL запрос
docker-compose exec postgres psql -U postgres -d real_estate_bot -c "SELECT * FROM users;"

# Бэкап БД
docker-compose exec postgres pg_dump -U postgres real_estate_bot > backup.sql

# Восстановление БД
docker-compose exec -T postgres psql -U postgres real_estate_bot < backup.sql
```

## Разработка

### Следующие шаги (см. план)

День 2-10: Реализация функционала согласно плану в `/Users/dmitriigrigorev/.claude/plans/stateful-hopping-valiant.md`

### Полезные команды

```bash
# Собрать проект
./gradlew build

# Запустить тесты
./gradlew test

# Очистить build
./gradlew clean
```

## Troubleshooting

### Docker

**Ошибка: "Cannot connect to the Docker daemon"**
- Убедитесь, что Docker Desktop запущен
- На Linux: `sudo systemctl start docker`

**Контейнер бота постоянно перезапускается**
- Проверьте логи: `docker-compose logs bot`
- Убедитесь, что все переменные в `.env` заполнены корректно
- Проверьте, что PostgreSQL запустился: `docker-compose ps`

**Порт 5432 уже занят**
- Измените порт в `.env`: `DB_PORT=5433`
- Или остановите локальный PostgreSQL: `brew services stop postgresql` (macOS)

### Локальная установка

**Ошибка подключения к БД**

Проверьте:
1. PostgreSQL запущен: `pg_isready`
2. База данных создана: `psql -U postgres -l | grep real_estate_bot`
3. Правильные credentials в `.env`

### Бот не отвечает

Проверьте:
1. BOT_TOKEN корректный
2. Бот запущен (видны логи в консоли)
3. Нет ошибок в логах

### Apify API ошибки

Проверьте:
1. APIFY_API_KEY корректный
2. Достаточно credits на аккаунте Apify
3. Rate limits не превышены

## Поддержка

Для вопросов и проблем создайте issue в репозитории или свяжитесь с разработчиком.

## Лицензия

MIT License
# casa-amigo
