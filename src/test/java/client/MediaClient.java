package client;

import api.BaseApi;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import models.MediaRequest;

import java.io.File;

/**
 * Клиент для работы с API медиафайлов WordPress (endpoint: /wp/v2/media)
 */
@Slf4j
public class MediaClient extends BaseApi {

    @Override
    protected String getBasePath() {
        return "wp-json/wp/v2/media";
    }

    @Step("Загрузка изображения")
    public Response uploadMedia(File file, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .contentType("multipart/form-data")
                .multiPart("file", file)
                .post();

        log.info("Загрузка изображения: статус={}, id={}", response.statusCode(), response.jsonPath().get("id"));
        return response;
    }

    @Step("Получение информации о медиафайле по ID: {id}")
    public Response getMediaById(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get("/" + id);

        log.info("Получение медиафайла ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Обновление alt_text медиафайла ID: {id}")
    public Response updateAltText(int id, String altText, AuthType authType, String... credentials) {
        MediaRequest request = new MediaRequest();
        request.setAltText(altText);

        Response response = getRequest(authType, credentials)
                .body(request)
                .put("/" + id);

        log.info("Обновление alt_text ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Обновление заголовка медиафайла ID: {id}")
    public Response updateTitle(int id, String title, AuthType authType, String... credentials) {
        MediaRequest request = new MediaRequest();
        request.setTitle(title);

        Response response = getRequest(authType, credentials)
                .body(request)
                .put("/" + id);

        log.info("Обновление заголовка ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Удаление медиафайла ID: {id}")
    public Response deleteMedia(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .delete("/" + id + "?force=true");

        log.info("Удаление медиафайла ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Загрузка без файла (негативный тест)")
    public Response uploadMediaWithoutFile(AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .contentType("multipart/form-data")
                .post();

        log.info("Загрузка без файла: статус={}", response.statusCode());
        return response;
    }
}