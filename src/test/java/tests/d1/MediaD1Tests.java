package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.MediaClient;
import db.service.MediaDbService;
import enums.TestDataTemplates;
import helpers.FileHelper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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

    private final List<Integer> mediaIds = new ArrayList<>();

    private static final String TEST_IMAGE_PATH = "src/test/resources/test.png";
    private static final String TEST_EXE_PATH = "src/test/resources/test.exe";

    private String mediaTitle, mediaAltText;

    @BeforeMethod
    public void create() {
        mediaTitle = TestDataTemplates.MEDIA_TITLE.getUniqueValue();
        mediaAltText = TestDataTemplates.MEDIA_ALT_TEXT.getUniqueValue();
    }

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

        Checks.checkStatusCode(response, 201);
        Checks.checkJsonFieldNotNull(response, "id");
        Checks.checkJsonFieldNotNull(response, "source_url");
        Checks.checkJsonField(response, "media_type", "image");

        int id = response.jsonPath().getInt("id");
        mediaIds.add(id);

        Checks.checkEquals(mediaDbService.isMediaExists(id), true, "Медиафайл должен существовать в БД");
        Checks.checkEquals(mediaDbService.hasMetadata(id), true, "Метаданные _wp_attachment_metadata должны присутствовать");
        Checks.checkTrue(mediaDbService.getMimeType(id).startsWith("image/"), "MIME тип должен начинаться с image/");
    }

    @Test(description = "TC-MEDIA-02: Получение информации о медиафайле по ID", groups = {"needsCleanup"})
    @Story("Получение медиафайла")
    public void tcMedia02_getMediaById() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        String expectedMimeType = mediaDbService.getMimeType(id);

        Response response = mediaClient.getMediaById(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "id", id);
        Checks.checkJsonField(response, "mime_type", expectedMimeType);

        Checks.checkEquals(mediaDbService.getMimeType(id), expectedMimeType, "MIME тип в БД должен совпадать");
    }

    @Test(description = "TC-MEDIA-03: Обновление alt_text у изображения", groups = {"needsCleanup"})
    @Story("Обновление медиафайла")
    public void tcMedia03_updateAltText() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        Response response = mediaClient.updateAltText(id, mediaAltText, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "alt_text", mediaAltText);

        Checks.checkEquals(mediaDbService.getAltText(id), mediaAltText, "alt_text в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-04: Обновление заголовка (title) медиафайла", groups = {"needsCleanup"})
    @Story("Обновление медиафайла")
    public void tcMedia04_updateTitle() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        Response response = mediaClient.updateTitle(id, mediaTitle, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "title.raw", mediaTitle);

        Checks.checkEquals(mediaDbService.getMediaTitle(id), mediaTitle, "Заголовок в БД должен обновиться");
    }

    @Test(description = "TC-MEDIA-05: Удаление медиафайла (force=true)", groups = {"needsCleanup"})
    @Story("Удаление медиафайла")
    public void tcMedia05_deleteMedia() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response createResponse = mediaClient.uploadMedia(file, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        mediaIds.add(id);

        Checks.checkEquals(mediaDbService.isMediaExists(id), true, "Медиафайл должен существовать до удаления");

        Response response = mediaClient.deleteMedia(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkDeletedTrue(response);

        Checks.checkEquals(mediaDbService.isMediaExists(id), false, "Медиафайл не должен существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-MEDIA-N01: Загрузка без файла")
    @Story("Загрузка медиафайла")
    public void tcMediaN01_uploadWithoutFile() {
        Response response = mediaClient.uploadMediaWithoutFile(AuthType.ADMIN);

        Checks.checkStatusCode(response, 400);
    }

    @Test(description = "TC-MEDIA-N02: Загрузка неавторизованным пользователем")
    @Story("Авторизация")
    public void tcMediaN02_uploadUnauthorized() {
        File file = FileHelper.getTestFile(TEST_IMAGE_PATH);

        Response response = mediaClient.uploadMedia(file, AuthType.NONE);

        Checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-MEDIA-N03: Загрузка файла недопустимого типа (.exe)")
    @Story("Загрузка медиафайла")
    public void tcMediaN03_uploadInvalidFileType() {
        File file = FileHelper.getTestFile(TEST_EXE_PATH);

        Response response = mediaClient.uploadMedia(file, AuthType.ADMIN);

        Checks.checkStatusCode(response, 415);
    }

    @Test(description = "TC-MEDIA-N04: Получение несуществующего медиафайла")
    @Story("Получение медиафайла")
    public void tcMediaN04_getNonExistentMedia() {
        Response response = mediaClient.getMediaById(999999, AuthType.ADMIN);
        Checks.checkStatusCode(response, 404);
    }
}