package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import client.UsersClient;
import db.service.UserDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.UserRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D1 тесты для API пользователей WordPress (endpoint: /wp/v2/users).
 */
@Epic("WordPress API Тестирование")
@Feature("Users (Пользователи) - D1 CRUD")
public class UsersD1Tests {

    UsersClient usersClient = new UsersClient();
    PostsClient postsClient = new PostsClient();
    UserDbService userDbService = new UserDbService();
    Checks checks = new Checks();

    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> postIds = new ArrayList<>();

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() {
        if (!postIds.isEmpty()) {
            for (int postId : postIds) {
                postsClient.deletePost(postId, AuthType.ADMIN);
            }
            postIds.clear();
        }

        userDbService.deleteUsersHard(userIds);
        userIds.clear();
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-USER-01: Создание нового пользователя (минимальные поля)", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUser01_createUserMinimalFields() {
        UserRequest request = new UserRequest("testuser", "test@example.com", "Test123!", null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonField(response, "username", request.getUsername());
        checks.checkJsonField(response, "email", request.getEmail());

        int id = response.jsonPath().getInt("id");
        userIds.add(id);

        checks.checkTrue(userDbService.isUserExists(id), "Пользователь должен существовать в БД");
        checks.checkEquals(userDbService.getUserUsername(id), request.getUsername(), "Username в БД");
        checks.checkEquals(userDbService.getUserEmail(id), request.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-02: Получение другого пользователя по ID", groups = {"needsCleanup"})
    @Story("Получение пользователей")
    public void tcUser02_getUserById() {
        UserRequest createRequest = new UserRequest("testuser_get", "testget@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        Response response = usersClient.getUserById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);

        checks.checkEquals(userDbService.getUserEmail(id), createRequest.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-03: Обновление email пользователя", groups = {"needsCleanup"})
    @Story("Обновление пользователя")
    public void tcUser03_updateUserEmail() {
        UserRequest createRequest = new UserRequest("testuser_update", "old@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        String body = "{\"email\": \"newemail@example.com\"}";
        Response response = usersClient.updateUserRaw(id, body, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "email", "newemail@example.com");

        checks.checkEquals(userDbService.getUserEmail(id), "newemail@example.com", "Email в БД");
    }

    @Test(description = "TC-USER-04: Удаление пользователя с reassign", groups = {"needsCleanup"})
    public void tcUser04_deleteUserWithReassign() {
        UserRequest request = new UserRequest("usertodelete", "delete@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        Response response = usersClient.deleteUser(id, 1, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);
        checks.checkFalse(userDbService.isUserExists(id), "Пользователь должен быть удален");
    }

    @Test(description = "TC-USER-05: У удаленного пользователя нет постов", groups = {"needsCleanup"})
    public void tcUser05_postsRemovedFromDeletedUser() {

        UserRequest request = new UserRequest("userposts", "posts@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        for (int i = 0; i < 3; i++) {
            String postBody = String.format(
                    "{\"title\":\"Test Post %d\",\"content\":\"Content %d\",\"status\":\"publish\",\"author\":%d}",
                    i, i, userId);

            Response postResponse = postsClient.createPostRaw(postBody, AuthType.ADMIN);
            checks.checkStatusCode(postResponse, 201);

            postIds.add(postResponse.jsonPath().getInt("id"));
        }

        usersClient.deleteUser(userId, 1, AuthType.ADMIN);

        for (int postId : postIds) {
            int author = userDbService.getPostAuthorId(postId);
            checks.checkTrue(author != userId, "Пост не должен принадлежать удаленному пользователю");
        }
    }

    @Test(description = "TC-USER-06: Посты переназначены новому автору", groups = {"needsCleanup"})
    public void tcUser06_postsReassigned() {

        UserRequest request = new UserRequest("userreassign", "reassign@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        for (int i = 0; i < 3; i++) {
            String postBody = String.format(
                    "{\"title\":\"Test Post %d\",\"content\":\"Content %d\",\"status\":\"publish\",\"author\":%d}",
                    i, i, userId);

            Response postResponse = postsClient.createPostRaw(postBody, AuthType.ADMIN);
            checks.checkStatusCode(postResponse, 201);

            postIds.add(postResponse.jsonPath().getInt("id"));
        }

        int reassignToId = 1;

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        for (int postId : postIds) {
            int author = userDbService.getPostAuthorId(postId);
            checks.checkEquals(author, reassignToId, "Пост должен быть переназначен новому автору");
        }
    }

    @Test(description = "TC-USER-07: У каждого поста обновился автор", groups = {"needsCleanup"})
    public void tcUser07_eachPostHasNewAuthor() {

        UserRequest request = new UserRequest("usercheckposts", "check@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        for (int i = 0; i < 3; i++) {
            String postBody = String.format(
                    "{\"title\":\"Test Post %d\",\"content\":\"Content %d\",\"status\":\"publish\",\"author\":%d}",
                    i, i, userId);

            Response postResponse = postsClient.createPostRaw(postBody, AuthType.ADMIN);
            checks.checkStatusCode(postResponse, 201);

            postIds.add(postResponse.jsonPath().getInt("id"));
        }

        int reassignToId = 1;

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        for (int postId : postIds) {

            Response postResponse = postsClient.getPostById(postId, AuthType.ADMIN);
            checks.checkStatusCode(postResponse, 200);
            checks.checkEquals(postResponse.jsonPath().getInt("author"), reassignToId, "Автор должен обновиться через API");
            checks.checkEquals(userDbService.getPostAuthorId(postId), reassignToId, "Автор должен обновиться в БД");
        }
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-USER-N01: Создание пользователя без пароля")
    @Story("Создание пользователя")
    public void tcUserN01_createUserWithoutPassword() {
        int beforeCount = userDbService.getUsersCount();

        UserRequest request = new UserRequest("nopass", "nopass@ex.com", null, null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(response, 400);

        int afterCount = userDbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Новый пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N02: Создание пользователя с уже существующим email", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUserN02_createUserDuplicateEmail() {
        UserRequest request1 = new UserRequest("user1", "duplicate@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        userIds.add(createResponse.jsonPath().getInt("id"));

        int beforeCount = userDbService.getUsersCount();

        UserRequest request2 = new UserRequest("user2", "duplicate@example.com", "Test123!", null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = userDbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N03: Создание пользователя с уже существующим username", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUserN03_createUserDuplicateUsername() {
        UserRequest request1 = new UserRequest("duplicate_user", "first@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        userIds.add(createResponse.jsonPath().getInt("id"));

        int beforeCount = userDbService.getUsersCount();

        UserRequest request2 = new UserRequest("duplicate_user", "second@example.com", "Test123!", null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = userDbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N04: Получение пользователя без авторизации")
    @Story("Авторизация")
    public void tcUserN04_getCurrentUserUnauthorized() {
        Response response = usersClient.getCurrentUserWithoutAuth();
        checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-USER-N05: Удаление пользователя без указания reassign", groups = {"needsCleanup"})
    @Story("Удаление пользователя")
    public void tcUserN05_deleteUserWithoutReassign() {
        UserRequest request = new UserRequest("user_noreassign", "noreassign@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        checks.checkEquals(userDbService.isUserExists(id), true, "Пользователь должен существовать до попытки удаления");

        Response response = usersClient.deleteUserWithoutReassign(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 400);
        checks.checkEquals(userDbService.isUserExists(id), true, "Пользователь должен остаться в БД");
    }

    @Test(description = "TC-USER-N06: Создание пользователя не-админом")
    @Story("Авторизация")
    public void tcUserN06_createUserNotAdmin() {
        UserRequest request = new UserRequest("nonadmin_user", "nonadmin@example.com", "Test123!", null, null);

        Response response = usersClient.createUser(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        checks.checkStatusCode(response, 403);
    }
}