package tests;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import db.PostDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.PostRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;

/**
 * Набор тестов для API записей WordPress (endpoint: /wp/v2/posts).
 */
@Epic("WordPress API Тестирование")
@Feature("Posts (Записи)")
public class PostsTests {

    PostsClient postsClient = new PostsClient();
    PostDbService dbService = new PostDbService();
    Checks checks = new Checks();

    private int createdPostId = -1;

    @BeforeMethod
    public void setUp() {
        createdPostId = -1;
    }

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() throws SQLException {
        if (createdPostId != -1 && dbService.isPostExists(createdPostId)) {
            dbService.deletePostHard(createdPostId);
        }
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-POST-01: Создание записи с минимальными полями", groups = {"needsCleanup"})
    @Story("Создание записи")
    public void tcPost01_createPostMinimalFields() throws SQLException {
        PostRequest request = new PostRequest("Test Post", "Test content", "draft");

        Response response = postsClient.createPost(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonField(response, "title.raw", request.getTitle());
        checks.checkJsonField(response, "status", request.getStatus());

        int id = response.jsonPath().getInt("id");
        createdPostId = id;

        checks.checkEquals(dbService.isPostExists(id), true, "Запись должна существовать в БД");
        checks.checkEquals(dbService.getPostTitle(id), request.getTitle(), "Заголовок в БД");
        checks.checkEquals(dbService.getPostContent(id), request.getContent(), "Содержимое в БД");
        checks.checkEquals(dbService.getPostStatus(id), request.getStatus(), "Статус в БД");
        checks.checkEquals(dbService.getPostType(id), "post", "Тип записи в БД");
    }

    @Test(description = "TC-POST-02: Получение списка записей (GET)", groups = {"needsCleanup"})
    @Story("Получение записей")
    public void tcPost02_getPostsList() throws SQLException {
        PostRequest testRequest = new PostRequest("List Test Post", "Content for list test", "publish");
        Response createResponse = postsClient.createPost(testRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdPostId = createResponse.jsonPath().getInt("id");

        Response response = postsClient.getPosts(AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkListNotEmpty(response, "");

        int responseCount = response.jsonPath().getList("").size();
        int dbCount = dbService.getPostsCount();

        checks.checkEquals(responseCount, dbCount, "Количество записей в ответе и БД");
    }

    @Test(description = "TC-POST-03: Получение одной записи по ID", groups = {"needsCleanup"})
    @Story("Получение записей")
    public void tcPost03_getPostById() throws SQLException {
        PostRequest request = new PostRequest("Test Post for Get", "Content for get", "publish");

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdPostId = id;

        Response response = postsClient.getPostById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);
        checks.checkJsonField(response, "title.rendered", request.getTitle());

        checks.checkEquals(dbService.getPostTitle(id), request.getTitle(), "Заголовок в БД");
        checks.checkEquals(dbService.getPostContent(id), request.getContent(), "Содержимое в БД");
    }

    @Test(description = "TC-POST-04: Обновление заголовка записи", groups = {"needsCleanup"})
    @Story("Обновление записи")
    public void tcPost04_updatePostTitle() throws SQLException {
        PostRequest createRequest = new PostRequest("Original Title", "Original content", "draft");

        Response createResponse = postsClient.createPost(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdPostId = id;

        String oldContent = dbService.getPostContent(id);
        String oldStatus = dbService.getPostStatus(id);

        PostRequest updateRequest = new PostRequest();
        updateRequest.setTitle("Updated Title");

        Response response = postsClient.updatePost(id, updateRequest, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "title.raw", "Updated Title");

        checks.checkEquals(dbService.getPostTitle(id), "Updated Title", "Заголовок в БД должен обновиться");
        checks.checkEquals(dbService.getPostContent(id), oldContent, "Содержимое не должно измениться");
        checks.checkEquals(dbService.getPostStatus(id), oldStatus, "Статус не должен измениться");
    }

    @Test(description = "TC-POST-05: Полное удаление записи")
    @Story("Удаление записи")
    public void tcPost05_deletePostForce() throws SQLException {
        PostRequest request = new PostRequest("Post to Delete", "This will be deleted", "draft");

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        createdPostId = createResponse.jsonPath().getInt("id");

        checks.checkEquals(dbService.isPostExists(createdPostId), true, "Запись должна существовать до удаления");

        Response response = postsClient.deletePost(createdPostId, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);
        checks.checkEquals(dbService.isPostExists(createdPostId), false, "Запись не должна существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-POST-N01: Создание записи без заголовка", groups = {"needsCleanup"})
    @Story("Создание записи")
    public void tcPostN01_createPostWithoutTitle() throws SQLException {
        PostRequest request = new PostRequest();
        request.setContent("No title content");
        request.setStatus("draft");

        Response response = postsClient.createPost(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);

        int id = response.jsonPath().getInt("id");
        createdPostId = id;

        checks.checkJsonFieldNullOrEmpty(response, "title.raw");

        checks.checkEquals(dbService.isPostExists(id), true, "Запись должна существовать в БД");

        String dbTitle = dbService.getPostTitle(id);
        checks.checkEquals(dbTitle == null || dbTitle.isEmpty(), true,
                "Заголовок в БД должен быть null или пустым");
    }

    @Test(description = "TC-POST-N02: Создание записи без авторизации")
    @Story("Авторизация")
    public void tcPostN02_createPostUnauthorized() {
        PostRequest request = new PostRequest("Unauthorized Post", "Should not be created", "draft");

        Response response = postsClient.createPost(request, AuthType.NONE);

        checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-POST-N03: Получение несуществующей записи")
    @Story("Получение записей")
    public void tcPostN03_getNonExistentPost() {
        Response response = postsClient.getPostById(999999, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N04: Обновление несуществующей записи")
    @Story("Обновление записи")
    public void tcPostN04_updateNonExistentPost() {
        PostRequest request = new PostRequest();
        request.setTitle("Updated Title");

        Response response = postsClient.updatePost(999999, request, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N05: Удаление несуществующей записи")
    @Story("Удаление записи")
    public void tcPostN05_deleteNonExistentPost() {
        Response response = postsClient.deletePost(999999, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N06: Создание поста без прав")
    @Story("Авторизация")
    public void tcPostN06_createPostWithoutRights() {
        PostRequest request = new PostRequest("No Rights Post", "Content", "publish");

        Response response = postsClient.createPost(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        checks.checkStatusCode(response, 403);
    }

    @Test(description = "TC-POST-N07: Удаление чужого поста", groups = {"needsCleanup"})
    @Story("Авторизация")
    public void tcPostN07_deleteSomeoneElsePost() throws SQLException {
        PostRequest request = new PostRequest("Admin Post", "Created by admin", "draft");

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdPostId = id;

        checks.checkEquals(dbService.isPostExists(id), true, "Запись должна существовать");

        Response response = postsClient.deletePost(id, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        checks.checkStatusCode(response, 403);
        checks.checkEquals(dbService.isPostExists(id), true, "Запись должна остаться в БД");
    }
}