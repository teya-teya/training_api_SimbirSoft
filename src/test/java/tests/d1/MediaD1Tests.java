package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.MediaClient;
import db.service.MediaDbService;
import helpers.FileHelper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * D1 тесты для API медиафайлов WordPress (endpoint: /wp/v2/media).
 */
@Epic("WordPress API Тестирование")
@Feature("Media (Медиафайлы) - D1 CRUD")
public class MediaD1Tests {

    MediaClient mediaClient = new MediaClient();
    MediaDbService mediaDbService = new MediaDbService();
    Checks checks = new Checks();

    private final List<Integer> mediaIds = new ArrayList<>();

    private static final String TEST_IMAGE_PATH = "src/test/resources/test.png";
    private static final String TEST_EXE_PATH = "src/test/resources/test.exe";

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() {
        mediaDbService.deleteMediasHard(mediaIds);
        mediaIds.clear();
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-MEDIA-01: Загрузка изображения (multipart)", groups = {"needsCleanup"})
    @Story("Загрузка медиафайла")
    public void tcMedia01_uploadImage() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response response = mediaClient.uploadMedia(file, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonFieldNotNull(response, "source_url");
        checks.checkJsonField(response, "media_type", "image");

        int id = response.jsonPath().getInt("id");
        mediaIds.add(id);

        checks.checkEquals(mediaDbService.isMediaExists(id), true, "Медиафайл должен существовать в БД");
        checks.checkEquals(mediaDbService.hasMetadata(id), true, "Метаданные _wp_attachment_metadata должны присутствовать");
        checks.checkTrue(mediaDbService.getMimeType(id).startsWith("image/"), "MIME тип должен начинаться с image/");
    }

    @Test(description = "TC-MEDIA-02: Получение информации о медиафайле по ID", groups = {"needsCleanup"})
    @Story("Получение медиафайла")
    public void tcMedia02_getMediaById() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        String expectedMimeType = mediaDbService.getMimeType(id);

        Response response = mediaClient.getMediaById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);
        checks.checkJsonField(response, "mime_type", expectedMimeType);

        checks.checkEquals(mediaDbService.getMimeType(id), expectedMimeType, "MIME тип в БД должен совпадать");
    }

    @Test(description = "TC-MEDIA-03: Обновление alt_text у изображения", groups = {"needsCleanup"})
    @Story("Обновление медиафайла")
    public void tcMedia03_updateAltText() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        Response response = mediaClient.updateAltText(id, "New alt text", AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "alt_text", "New alt text");

        checks.checkEquals(mediaDbService.getAltText(id), "New alt text", "alt_text в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-04: Обновление заголовка (title) медиафайла", groups = {"needsCleanup"})
    @Story("Обновление медиафайла")
    public void tcMedia04_updateTitle() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        Response response = mediaClient.updateTitle(id, "New Title", AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "title.raw", "New Title");

        checks.checkEquals(mediaDbService.getMediaTitle(id), "New Title", "Заголовок в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-05: Удаление медиафайла (force=true)", groups = {"needsCleanup"})
    @Story("Удаление медиафайла")
    public void tcMedia05_deleteMedia() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        checks.checkEquals(mediaDbService.isMediaExists(id), true, "Медиафайл должен существовать до удаления");

        Response response = mediaClient.deleteMedia(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);

        checks.checkEquals(mediaDbService.isMediaExists(id), false, "Медиафайл не должен существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-MEDIA-N01: Загрузка без файла")
    @Story("Загрузка медиафайла")
    public void tcMediaN01_uploadWithoutFile() {
        Response response = mediaClient.uploadMediaWithoutFile(AuthType.ADMIN);

        checks.checkStatusCode(response, 400);
    }

    @Test(description = "TC-MEDIA-N02: Загрузка неавторизованным пользователем")
    @Story("Авторизация")
    public void tcMediaN02_uploadUnauthorized() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response response = mediaClient.uploadMedia(file, AuthType.NONE);

        checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-MEDIA-N03: Загрузка файла недопустимого типа (.exe)")
    @Story("Загрузка медиафайла")
    public void tcMediaN03_uploadInvalidFileType() {
        File file = FileHelper.getTestFile(TEST_EXE_PATH);

        Response response = mediaClient.uploadMedia(file, AuthType.ADMIN);

        checks.checkStatusCode(response, 415);
    }

    @Test(description = "TC-MEDIA-N04: Получение несуществующего медиафайла")
    @Story("Получение медиафайла")
    public void tcMediaN04_getNonExistentMedia() {
        Response response = mediaClient.getMediaById(999999, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }
}