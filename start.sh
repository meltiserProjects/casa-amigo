#!/bin/bash

# Скрипт для быстрого запуска Real Estate Bot

set -e

echo "🏠 Real Estate Bot - Quick Start"
echo "================================"
echo ""

# Проверка наличия .env файла
if [ ! -f .env ]; then
    echo "⚠️  Файл .env не найден!"
    echo ""
    echo "Копирую .env.example в .env..."
    cp .env.example .env
    echo ""
    echo "✅ Файл .env создан. Пожалуйста, отредактируйте его и добавьте:"
    echo "   - BOT_TOKEN (от @BotFather)"
    echo "   - APIFY_API_KEY (от apify.com)"
    echo ""
    echo "После редактирования запустите скрипт снова."
    exit 1
fi

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен!"
    echo "   Установите Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose не установлен!"
    exit 1
fi

# Проверка, что Docker запущен
if ! docker info &> /dev/null; then
    echo "❌ Docker не запущен!"
    echo "   Запустите Docker Desktop и попробуйте снова."
    exit 1
fi

echo "✅ Docker проверен"
echo ""

# Проверка переменных окружения
source .env

if [ -z "$BOT_TOKEN" ] || [ "$BOT_TOKEN" = "your_telegram_bot_token_from_botfather" ]; then
    echo "⚠️  BOT_TOKEN не настроен в .env!"
    echo "   Получите токен у @BotFather в Telegram"
    exit 1
fi

if [ -z "$APIFY_API_KEY" ] || [ "$APIFY_API_KEY" = "your_apify_api_key" ]; then
    echo "⚠️  APIFY_API_KEY не настроен в .env!"
    echo "   Получите ключ на https://apify.com"
    exit 1
fi

echo "✅ Переменные окружения настроены"
echo ""

# Сборка приложения
echo "🔨 Сборка приложения..."
echo ""

./gradlew clean bootJar --no-daemon

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Ошибка сборки приложения!"
    exit 1
fi

echo ""
echo "✅ Приложение собрано"
echo ""

# Запуск Docker Compose
echo "🚀 Запуск сервисов..."
echo ""

docker-compose up -d --build

echo ""
echo "✅ Сервисы запущены!"
echo ""
echo "📊 Проверить статус:"
echo "   docker-compose ps"
echo ""
echo "📋 Просмотр логов:"
echo "   docker-compose logs -f bot"
echo ""
echo "🛑 Остановка:"
echo "   docker-compose down"
echo ""
echo "🎉 Бот готов к работе! Найдите его в Telegram и отправьте /start"
