package client;

import api.BaseApi;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import models.PostRequest;

/**
 * Клиент для работы с API записей WordPress (endpoint: /wp/v2/posts)
 */
@Slf4j
public class PostsClient extends BaseApi {

    @Override
    protected String getBasePath() {
        return "wp-json/wp/v2/posts";
    }

    @Step("Создание новой записи")
    public Response createPost(PostRequest request, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(request)
                .post();

        log.info("Создание записи: статус={}, id={}", response.statusCode(), response.jsonPath().get("id"));
        return response;
    }

    @Step("Получение списка записей")
    public Response getPosts(AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .queryParam("status", "any")
                .get();

        log.info("Получение списка записей: статус={}, количество={}", response.statusCode(), response.jsonPath().getList("").size());
        return response;
    }

    @Step("Получение записи по ID: {id}")
    public Response getPostById(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .get("/" + id);

        log.info("Получение записи ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Обновление записи ID: {id}")
    public Response updatePost(int id, PostRequest request, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(request)
                .put("/" + id);

        log.info("Обновление записи ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Удаление записи ID: {id}")
    public Response deletePost(int id, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .delete("/" + id + "?force=true");

        log.info("Удаление записи ID={}: статус={}", id, response.statusCode());
        return response;
    }

    @Step("Создание записи с указанием автора (raw JSON)")
    public Response createPostRaw(String body, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .body(body)
                .post();

        Integer id = response.jsonPath().get("id");
        log.info("Создание записи с автором: статус={}, id={}", response.statusCode(), id != null ? id : "null");
        return response;
    }

    @Step("Получение записей по статусу: {status}")
    public Response getPostsByStatus(String status, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .queryParam("status", status)
                .get();

        log.info("Получение записей по статусу={}: статус={}, количество={}", status, response.statusCode(), response.jsonPath().getList("").size());
        return response;
    }

    @Step("Получение записей по автору: {authorId}")
    public Response getPostsByAuthor(int authorId, AuthType authType, String... credentials) {
        Response response = getRequest(authType, credentials)
                .queryParam("author", authorId)
                .get();

        log.info("Получение записей по автору={}: статус={}, количество={}", authorId, response.statusCode(), response.jsonPath().getList("").size());
        return response;
    }
}