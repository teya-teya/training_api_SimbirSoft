package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.MediaClient;
import db.service.MediaDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D2 тесты для API медиафайлов WordPress (endpoint: /wp/v2/media).
 */
@Epic("WordPress API Тестирование")
@Feature("Media (Медиафайлы) - D2 GET запросы")
public class MediaD2Tests {

    MediaClient mediaClient = new MediaClient();
    MediaDbService mediaDbService = new MediaDbService();
    Checks checks = new Checks();

    private final List<Integer> mediaIds = new ArrayList<>();

    @AfterMethod
    public void cleanUp() {
        mediaDbService.deleteMediasHard(mediaIds);
        mediaIds.clear();
    }

    @Test(description = "TC-MEDIA-01-D2: Получение медиа по ID")
    public void tcMedia01D2_getMediaById() {
        int mediaId = mediaDbService.createTestAttachment("image/png", "autotest");
        mediaIds.add(mediaId);

        Response response = mediaClient.getMediaById(mediaId, AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        String mediaType = response.jsonPath().getString("media_type");
        checks.checkTrue(mediaType.equals("image") || mediaType.equals("file"),
                "media_type должен быть 'image' или 'file', получено: " + mediaType);

        checks.checkEquals(response.jsonPath().getString("mime_type"), "image/png", "mime_type должен быть image/png");

        checks.checkEquals(mediaDbService.getMediaMimeType(mediaId), "image/png", "MIME type в БД должен совпадать");
        checks.checkEquals(mediaDbService.getMediaPostType(mediaId), "attachment", "post_type в БД должен быть attachment");
    }
}