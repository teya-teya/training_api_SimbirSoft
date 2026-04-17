package client;

import api.BaseApi;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import models.UserRequest;

/**
 * Клиент для работы с API пользователей WordPress (endpoint: /wp/v2/users)
 */
@Slf4j
public class UsersClient extends BaseApi {

    @Override
    protected String getBasePath() {
        return "wp-json/wp/v2/users";
    }

    @Step("Создание нового пользователя")
    public Response createUser(UserRequest request, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(request)
                .post();

        Integer id = response.jsonPath().get("id");
        log.info("Создание пользователя: статус={}, id={}, username={}", response.statusCode(), id != null ? id : "null", request.getUsername());
        return response;
    }

    @Step("Получение пользователя по ID: {id}")
    public Response getUserById(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get("/" + id);

        log.info("Получение пользователя ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Обновление пользователя ID: {id} (raw JSON)")
    public Response updateUserRaw(int id, String body, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(body)
                .put("/" + id);

        log.info("Обновление пользователя ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Удаление пользователя ID: {id} с переназначением записей пользователю {reassign}")
    public Response deleteUser(int id, int reassign, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .delete("/" + id + "?force=true&reassign=" + reassign);

        log.info("Удаление пользователя ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Получение пользователя без авторизации")
    public Response getCurrentUserWithoutAuth() {
        Response response = getRequest(AuthType.NONE)
                .get("/me");

        log.info("Получение пользователя без авторизации: статус={}", response.statusCode());
        return response;
    }

    @Step("Удаление пользователя без reassign (негативный тест)")
    public Response deleteUserWithoutReassign(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .delete("/" + id + "?force=true");

        log.info("Удаление пользователя ID={} без reassign: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Получение списка пользователей")
    public Response getUsers(AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get();

        log.info("Получение списка пользователей: статус={}, количество={}", response.statusCode(), response.jsonPath().getList("").size());
        return response;
    }

    @Step("Получение пользователей по slug: {slug}")
    public Response getUsersBySlug(String slug, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .queryParam("slug", slug)
                .get();

        log.info("Получение пользователей по slug={}: статус={}", slug, response.statusCode());
        return response;
    }
}