package tests;

import api.BaseApi.AuthType;
import checks.Checks;
import client.MediaClient;
import db.MediaDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.SQLException;

/**
 * Набор тестов для API медиафайлов WordPress (endpoint: /wp/v2/media).
 */
@Epic("WordPress API Тестирование")
@Feature("Media (Медиафайлы)")
public class MediaTests {

    MediaClient mediaClient = new MediaClient();
    MediaDbService dbService = new MediaDbService();
    Checks checks = new Checks();

    private int createdMediaId = -1;

    private static final String TEST_IMAGE_PATH = "src/test/resources/test.png";
    private static final String TEST_EXE_PATH = "src/test/resources/test.exe";

    @BeforeMethod
    public void setUp() {
        createdMediaId = -1;
    }

    @AfterMethod
    public void cleanUp() throws SQLException {
        if (createdMediaId != -1 && dbService.isMediaExists(createdMediaId)) {
            dbService.deleteMediaHard(createdMediaId);
        }
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-MEDIA-01: Загрузка изображения (multipart)")
    @Story("Загрузка медиафайла")
    public void tcMedia01_uploadImage() throws SQLException {
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response response = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonFieldNotNull(response, "source_url");
        checks.checkJsonField(response, "media_type", "image");

        int id = response.jsonPath().getInt("id");
        createdMediaId = id;

        checks.checkEquals(dbService.isMediaExists(id), true, "Медиафайл должен существовать в БД");
        checks.checkEquals(dbService.hasMetadata(id), true, "Метаданные _wp_attachment_metadata должны присутствовать");
        checks.checkTrue(dbService.getMimeType(id).startsWith("image/"), "MIME тип должен начинаться с image/");
    }

    @Test(description = "TC-MEDIA-02: Получение информации о медиафайле по ID")
    @Story("Получение медиафайла")
    public void tcMedia02_getMediaById() throws SQLException {
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response createResponse = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdMediaId = id;

        String expectedMimeType = dbService.getMimeType(id);

        Response response = mediaClient.getMediaById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);
        checks.checkJsonField(response, "mime_type", expectedMimeType);

        checks.checkEquals(dbService.getMimeType(id), expectedMimeType, "MIME тип в БД должен совпадать");
    }

    @Test(description = "TC-MEDIA-03: Обновление alt_text у изображения")
    @Story("Обновление медиафайла")
    public void tcMedia03_updateAltText() throws SQLException {
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response createResponse = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdMediaId = id;

        Response response = mediaClient.updateAltText(id, "New alt text", AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "alt_text", "New alt text");

        checks.checkEquals(dbService.getAltText(id), "New alt text", "alt_text в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-04: Обновление заголовка (title) медиафайла")
    @Story("Обновление медиафайла")
    public void tcMedia04_updateTitle() throws SQLException {
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response createResponse = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdMediaId = id;

        Response response = mediaClient.updateTitle(id, "New Title", AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "title.raw", "New Title");

        checks.checkEquals(dbService.getMediaTitle(id), "New Title", "Заголовок в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-05: Удаление медиафайла (force=true)")
    @Story("Удаление медиафайла")
    public void tcMedia05_deleteMedia() throws SQLException {
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response createResponse = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdMediaId = id;

        checks.checkEquals(dbService.isMediaExists(id), true, "Медиафайл должен существовать до удаления");

        Response response = mediaClient.deleteMedia(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);

        checks.checkEquals(dbService.isMediaExists(id), false, "Медиафайл не должен существовать после удаления");
        createdMediaId = -1;
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
        File testImage = new File(TEST_IMAGE_PATH);
        if (!testImage.exists()) {
            throw new RuntimeException("Тестовое изображение не найдено: " + TEST_IMAGE_PATH);
        }

        Response response = mediaClient.uploadMedia(TEST_IMAGE_PATH, AuthType.NONE);

        checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-MEDIA-N03: Загрузка файла недопустимого типа (.exe)")
    @Story("Загрузка медиафайла")
    public void tcMediaN03_uploadInvalidFileType() {
        File invalidFile = new File(TEST_EXE_PATH);
        if (!invalidFile.exists()) {
            throw new RuntimeException("Тестовый .exe файл не найден: " + TEST_EXE_PATH);
        }

        Response response = mediaClient.uploadMedia(TEST_EXE_PATH, AuthType.ADMIN);

        checks.checkStatusCode(response, 415);
    }

    @Test(description = "TC-MEDIA-N04: Получение несуществующего медиафайла")
    @Story("Получение медиафайла")
    public void tcMediaN04_getNonExistentMedia() {
        Response response = mediaClient.getMediaById(999999, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }
}