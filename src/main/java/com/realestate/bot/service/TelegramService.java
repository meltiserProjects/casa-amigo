package com.realestate.bot.service;

import com.realestate.bot.model.dto.ListingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для отправки сообщений через Telegram
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    /**
     * Отправить объявление о квартире пользователю
     *
     * @param chatId ID чата
     * @param listing объявление
     * @param sender отправитель (бот)
     * @return true если успешно отправлено
     */
    public boolean sendListing(Long chatId, ListingDto listing, AbsSender sender) {
        log.debug("Sending listing to chatId {}: {}", chatId, listing.getIdealistaId());

        try {
            String messageText = formatListingMessage(listing);

            // Проверяем наличие фотографий
            List<String> photos = listing.getPhotoUrls();
            if (photos == null || photos.isEmpty()) {
                // Нет фотографий - отправляем только текст
                sendTextMessage(chatId, messageText, sender);
            } else if (photos.size() == 1) {
                // Одна фотография - отправляем SendPhoto
                sendPhotoMessage(chatId, messageText, photos.get(0), sender);
            } else {
                // Несколько фотографий - отправляем MediaGroup
                sendMediaGroup(chatId, messageText, photos, sender);
            }

            return true;

        } catch (Exception e) {
            log.error("Error sending listing to chatId {}: {}", chatId, listing.getIdealistaId(), e);
            return false;
        }
    }

    /**
     * Отправить список объявлений пользователю
     *
     * @param chatId ID чата
     * @param listings список объявлений
     * @param sender отправитель (бот)
     * @return количество успешно отправленных объявлений
     */
    public int sendListings(Long chatId, List<ListingDto> listings, AbsSender sender) {
        log.info("Sending {} listings to chatId {}", listings.size(), chatId);

        int successCount = 0;
        for (ListingDto listing : listings) {
            if (sendListing(chatId, listing, sender)) {
                successCount++;
                // Небольшая пауза между сообщениями, чтобы не превысить rate limit
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("Successfully sent {} out of {} listings to chatId {}",
                successCount, listings.size(), chatId);
        return successCount;
    }

    /**
     * Форматировать объявление для отображения в Telegram
     */
    private String formatListingMessage(ListingDto listing) {
        StringBuilder message = new StringBuilder();
        message.append("🏠 Новая квартира найдена!\n\n");

        if (listing.getPrice() != null) {
            message.append(String.format("💰 Цена: %,d EUR/мес\n", listing.getPrice()));
        }

        if (listing.getNumRooms() != null) {
            message.append(String.format("🛏 Комнат: %d\n", listing.getNumRooms()));
        }

        if (listing.getDistrict() != null && !listing.getDistrict().isEmpty()) {
            message.append(String.format("📍 Район: %s\n", listing.getDistrict()));
        }

        message.append("\n");

        if (listing.getDescription() != null && !listing.getDescription().isEmpty()) {
            // Ограничиваем длину описания
            String description = listing.getDescription();
            if (description.length() > 300) {
                description = description.substring(0, 297) + "...";
            }
            message.append(description);
            message.append("\n\n");
        }

        message.append(String.format("🔗 Ссылка: %s", listing.getIdealistaUrl()));

        return message.toString();
    }

    /**
     * Отправить текстовое сообщение
     */
    private void sendTextMessage(Long chatId, String text, AbsSender sender) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.disableWebPagePreview();
        sender.execute(message);
    }

    /**
     * Отправить сообщение с одной фотографией
     */
    private void sendPhotoMessage(Long chatId, String caption, String photoUrl, AbsSender sender)
            throws TelegramApiException {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId.toString());
        photo.setPhoto(new InputFile(photoUrl));
        photo.setCaption(caption);
        sender.execute(photo);
    }

    /**
     * Отправить группу фотографий (2-3 фото)
     */
    private void sendMediaGroup(Long chatId, String caption, List<String> photoUrls, AbsSender sender)
            throws TelegramApiException {

        // Telegram позволяет отправлять до 10 фото в MediaGroup, но мы берем первые 3
        List<InputMedia> mediaList = new ArrayList<>();
        int count = Math.min(photoUrls.size(), 3);

        for (int i = 0; i < count; i++) {
            InputMediaPhoto mediaPhoto = new InputMediaPhoto();
            mediaPhoto.setMedia(photoUrls.get(i));

            // Подпись добавляем только к первой фотографии
            if (i == 0) {
                mediaPhoto.setCaption(caption);
            }

            mediaList.add(mediaPhoto);
        }

        SendMediaGroup mediaGroup = new SendMediaGroup();
        mediaGroup.setChatId(chatId.toString());
        mediaGroup.setMedias(mediaList);

        sender.execute(mediaGroup);
    }

    /**
     * Отправить простое текстовое сообщение (публичный метод для общего использования)
     *
     * @param chatId ID чата
     * @param text текст сообщения
     * @param sender отправитель (бот)
     */
    public void sendMessage(Long chatId, String text, AbsSender sender) {
        try {
            sendTextMessage(chatId, text, sender);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chatId: {}", chatId, e);
        }
    }
}
