package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import db.service.PostDbService;
import enums.TestDataTemplates;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.PostRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D1 тесты для API записей WordPress (endpoint: /wp/v2/posts).
 */
@Epic("WordPress API Тестирование")
@Feature("Posts (Записи) - D1 CRUD")
public class PostsD1Tests {

    PostsClient postsClient = new PostsClient();
    PostDbService postDbService = new PostDbService();

    private final List<Integer> postIds = new ArrayList<>();
    private final String statusDraft = "draft";
    private final String statusPublic = "publish";
    private String postTitle, postContent;
    
    @BeforeMethod
    public void create() {
        postTitle = TestDataTemplates.POST_TITLE.getUniqueValue();
        postContent = TestDataTemplates.POST_CONTENT.getUniqueValue();
    }

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() {
        postDbService.deletePostsHard(postIds);
        postIds.clear();
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-POST-01: Создание записи с минимальными полями", groups = {"needsCleanup"})
    @Story("Создание записи")
    public void tcPost01_createPostMinimalFields() {
        PostRequest request = new PostRequest(postTitle, postContent, statusDraft);

        Response response = postsClient.createPost(request, AuthType.ADMIN);

        Checks.checkStatusCode(response, 201);
        Checks.checkJsonFieldNotNull(response, "id");
        Checks.checkJsonField(response, "title.raw", request.getTitle());
        Checks.checkJsonField(response, "status", request.getStatus());

        int id = response.jsonPath().getInt("id");
        postIds.add(id);

        Checks.checkEquals(postDbService.isPostExists(id), true, "Запись должна существовать в БД");
        Checks.checkEquals(postDbService.getPostTitle(id), request.getTitle(), "Заголовок в БД");
        Checks.checkEquals(postDbService.getPostContent(id), request.getContent(), "Содержимое в БД");
        Checks.checkEquals(postDbService.getPostStatus(id), request.getStatus(), "Статус в БД");
        Checks.checkEquals(postDbService.getPostType(id), "post", "Тип записи в БД");
    }

    @Test(description = "TC-POST-02: Получение списка записей (GET)", groups = {"needsCleanup"})
    @Story("Получение записей")
    public void tcPost02_getPostsList() {

        PostRequest testRequest = new PostRequest(postTitle, postContent, statusPublic);
        Response createResponse = postsClient.createPost(testRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        postIds.add(id);

        Response response = postsClient.getPosts(AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkListNotEmpty(response, "");

        List<Integer> ids = response.jsonPath().getList("id");

        Checks.checkTrue(ids.contains(id), "Созданный пост должен быть в списке");
    }

    @Test(description = "TC-POST-03: Получение одной записи по ID")
    @Story("Получение записей")
    public void tcPost03_getPostById() {
        PostRequest request = new PostRequest(postTitle, postContent, statusPublic);

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        postIds.add(id);

        Response response = postsClient.getPostById(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "id", id);
        Checks.checkJsonField(response, "title.rendered", request.getTitle());

        Checks.checkEquals(postDbService.getPostTitle(id), request.getTitle(), "Заголовок в БД");
        Checks.checkEquals(postDbService.getPostContent(id), request.getContent(), "Содержимое в БД");
    }

    @Test(description = "TC-POST-04: Обновление заголовка записи", groups = {"needsCleanup"})
    @Story("Обновление записи")
    public void tcPost04_updatePostTitle() {
        PostRequest createRequest = new PostRequest(postTitle, postContent, statusDraft);

        Response createResponse = postsClient.createPost(createRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        postIds.add(id);

        String oldContent = postDbService.getPostContent(id);
        String oldStatus = postDbService.getPostStatus(id);

        PostRequest updateRequest = new PostRequest();
        updateRequest.setTitle("Updated Title");

        Response response = postsClient.updatePost(id, updateRequest, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "title.raw", "Updated Title");

        Checks.checkEquals(postDbService.getPostTitle(id), "Updated Title", "Заголовок в БД должен обновиться");
        Checks.checkEquals(postDbService.getPostContent(id), oldContent, "Содержимое не должно измениться");
        Checks.checkEquals(postDbService.getPostStatus(id), oldStatus, "Статус не должен измениться");
    }

    @Test(description = "TC-POST-05: Полное удаление записи")
    @Story("Удаление записи")
    public void tcPost05_deletePostForce() {
        PostRequest request = new PostRequest(postTitle, postContent, statusDraft);

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        postIds.add(createResponse.jsonPath().getInt("id"));

        Checks.checkEquals(postDbService.isPostExists(postIds.get(0)), true, "Запись должна существовать до удаления");

        Response response = postsClient.deletePost(postIds.get(0), AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkDeletedTrue(response);
        Checks.checkEquals(postDbService.isPostExists(postIds.get(0)), false, "Запись не должна существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-POST-N01: Создание записи без заголовка", groups = {"needsCleanup"})
    @Story("Создание записи")
    public void tcPostN01_createPostWithoutTitle() {
        PostRequest request = new PostRequest();
        request.setContent(postContent);
        request.setStatus(statusDraft);

        Response response = postsClient.createPost(request, AuthType.ADMIN);

        Checks.checkStatusCode(response, 201);

        int id = response.jsonPath().getInt("id");
        postIds.add(id);

        Checks.checkJsonFieldNullOrEmpty(response, "title.raw");

        Checks.checkEquals(postDbService.isPostExists(id), true, "Запись должна существовать в БД");

        String dbTitle = postDbService.getPostTitle(id);
        Checks.checkEquals(dbTitle == null || dbTitle.isEmpty(), true,
                "Заголовок в БД должен быть null или пустым");
    }

    @Test(description = "TC-POST-N02: Создание записи без авторизации")
    @Story("Авторизация")
    public void tcPostN02_createPostUnauthorized() {
        PostRequest request = new PostRequest(postTitle, postContent, statusDraft);

        Response response = postsClient.createPost(request, AuthType.NONE);

        Checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-POST-N03: Получение несуществующей записи")
    @Story("Получение записей")
    public void tcPostN03_getNonExistentPost() {
        Response response = postsClient.getPostById(999999, AuthType.ADMIN);
        Checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N04: Обновление несуществующей записи")
    @Story("Обновление записи")
    public void tcPostN04_updateNonExistentPost() {
        PostRequest request = new PostRequest();
        request.setTitle(postTitle);

        Response response = postsClient.updatePost(999999, request, AuthType.ADMIN);
        Checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N05: Удаление несуществующей записи")
    @Story("Удаление записи")
    public void tcPostN05_deleteNonExistentPost() {
        Response response = postsClient.deletePost(999999, AuthType.ADMIN);
        Checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-POST-N06: Создание поста без прав")
    @Story("Авторизация")
    public void tcPostN06_createPostWithoutRights() {
        PostRequest request = new PostRequest(postTitle, postContent, statusPublic);

        Response response = postsClient.createPost(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        Checks.checkStatusCode(response, 403);
    }

    @Test(description = "TC-POST-N07: Удаление чужого поста", groups = {"needsCleanup"})
    @Story("Авторизация")
    public void tcPostN07_deleteSomeoneElsePost() {
        PostRequest request = new PostRequest(postTitle, postContent, statusDraft);

        Response createResponse = postsClient.createPost(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        postIds.add(id);

        Checks.checkEquals(postDbService.isPostExists(id), true, "Запись должна существовать");

        Response response = postsClient.deletePost(id, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        Checks.checkStatusCode(response, 403);
        Checks.checkEquals(postDbService.isPostExists(id), true, "Запись должна остаться в БД");
    }
}