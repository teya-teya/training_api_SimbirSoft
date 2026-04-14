package client;

import api.BaseApi;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import models.CategoryRequest;

/**
 * Клиент для работы с API рубрик WordPress (endpoint: /wp/v2/categories)
 */
@Slf4j
public class CategoriesClient extends BaseApi {

    @Override
    protected String getBasePath() {
        return "wp-json/wp/v2/categories";
    }

    @Step("Создание рубрики")
    public Response createCategory(CategoryRequest request, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(request)
                .post();

        log.info("Создание рубрики: статус={}, id={}, name={}", response.statusCode(), response.jsonPath().get("id"), request.getName());
        return response;
    }

    @Step("Получение списка рубрик")
    public Response getCategories(AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get();

        log.info("Получение рубрик: статус={}, количество={}", response.statusCode(), response.jsonPath().getList("").size());
        return response;
    }

    @Step("Получение рубрики по ID: {id}")
    public Response getCategoryById(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get("/" + id);

        log.info("Получение рубрики ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Обновление рубрики ID: {id}")
    public Response updateCategory(int id, CategoryRequest request, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(request)
                .put("/" + id);

        log.info("Обновление рубрики ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Удаление рубрики ID: {id}")
    public Response deleteCategory(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .delete("/" + id + "?force=true");

        log.info("Удаление рубрики ID={}: статус={}", id, response.statusCode());
        return response;
    }
}