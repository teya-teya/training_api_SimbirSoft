package db.service;

import db.dao.MediaDao;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_posts, wp_postmeta).
 */
@Slf4j
public class MediaDbService {

    private final MediaDao mediaDao = new MediaDao();

    @Step("Проверка существования медиафайла в БД по ID: {id}")
    public boolean isMediaExists(int id) {
        return mediaDao.fileExists(id);
    }

    @Step("Получение mime_type медиафайла из БД по ID: {id}")
    public String getMimeType(int id) {
        return mediaDao.getMimeType(id);
    }

    @Step("Получение заголовка медиафайла из БД по ID: {id}")
    public String getMediaTitle(int id) {
        return mediaDao.getTitle(id);
    }

    @Step("Получение alt_text медиафайла из БД по ID: {id}")
    public String getAltText(int id) {
        return mediaDao.getAltText(id);
    }

    @Step("Проверка наличия метаданных _wp_attachment_metadata у медиафайла ID: {id}")
    public boolean hasMetadata(int id) {
        return mediaDao.hasMetadata(id);
    }

    @Step("Создание тестового attachment в БД")
    public int createTestAttachment(String mimeType, String guidPattern) {
        return mediaDao.createTestAttachment(mimeType, guidPattern);
    }

    @Step("Получение MIME type медиафайла по ID")
    public String getMediaMimeType(int mediaId) {
        return mediaDao.getMimeType(mediaId);
    }

    @Step("Получение post_type медиафайла по ID")
    public String getMediaPostType(int mediaId) {
        return mediaDao.getPostType(mediaId);
    }

    @Step("Принудительное удаление медиафайла из БД по ID: {id}")
    public void deleteMediaHard(int id) {
        mediaDao.delete(id);
    }

    @Step("Удаление списка медиафайлов из БД по ID")
    public void deleteMediasHard(List<Integer> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        for (int mediaId : mediaIds) {
            try {
                deleteMediaHard(mediaId);
            } catch (Exception e) {
                log.warn("Не удалось удалить медиафайл {}", mediaId);
            }
        }
    }
}