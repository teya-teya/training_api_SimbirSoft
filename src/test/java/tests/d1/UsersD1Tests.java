package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import client.UsersClient;
import db.service.UserDbService;
import enums.TestDataTemplates;
import helpers.TestDataGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.UserRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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

    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> postIds = new ArrayList<>();

    private String username, email, password;
    private final String status = "publish";

    @BeforeMethod
    public void create() {
        username = TestDataTemplates.USER_USERNAME.getUniqueValue();
        email = TestDataGenerator.getRandomEmail();
        password = TestDataGenerator.getRandomPassword();
    }

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
        UserRequest request = new UserRequest(username, email, password, null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);

        Checks.checkStatusCode(response, 201);
        Checks.checkJsonFieldNotNull(response, "id");
        Checks.checkJsonField(response, "username", request.getUsername());
        Checks.checkJsonField(response, "email", request.getEmail());

        int id = response.jsonPath().getInt("id");
        userIds.add(id);

        Checks.checkTrue(userDbService.isUserExists(id), "Пользователь должен существовать в БД");
        Checks.checkEquals(userDbService.getUserUsername(id), request.getUsername(), "Username в БД");
        Checks.checkEquals(userDbService.getUserEmail(id), request.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-02: Получение другого пользователя по ID", groups = {"needsCleanup"})
    @Story("Получение пользователей")
    public void tcUser02_getUserById() {
        UserRequest createRequest = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        Response response = usersClient.getUserById(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "id", id);

        Checks.checkEquals(userDbService.getUserEmail(id), createRequest.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-03: Обновление email пользователя", groups = {"needsCleanup"})
    @Story("Обновление пользователя")
    public void tcUser03_updateUserEmail() {
        UserRequest createRequest = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        String body = "{\"email\": \"newemail@example.com\"}";
        Response response = usersClient.updateUserRaw(id, body, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "email", "newemail@example.com");

        Checks.checkEquals(userDbService.getUserEmail(id), "newemail@example.com", "Email в БД");
    }

    @Test(description = "TC-USER-04: Удаление пользователя с reassign", groups = {"needsCleanup"})
    public void tcUser04_deleteUserWithReassign() {
        UserRequest request = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        Response response = usersClient.deleteUser(id, 1, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkDeletedTrue(response);
        Checks.checkFalse(userDbService.isUserExists(id), "Пользователь должен быть удален");
    }

    @Test(description = "TC-USER-05: У удаленного пользователя нет постов", groups = {"needsCleanup"})
    public void tcUser05_postsRemovedFromDeletedUser() {

        UserRequest request = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        postIds.addAll(postsClient.createPostsForAuthor(3, userId, status, AuthType.ADMIN));
        usersClient.deleteUser(userId, 1, AuthType.ADMIN);

        Checks.checkNoPostBelongsToUser(postIds, userDbService::getPostAuthorId, userId,"Пост не должен принадлежать удаленному пользователю");
    }

    @Test(description = "TC-USER-06: Посты переназначены новому автору", groups = {"needsCleanup"})
    public void tcUser06_postsReassigned() {

        UserRequest request = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        postIds.addAll(postsClient.createPostsForAuthor(3, userId, status, AuthType.ADMIN));

        int reassignToId = 1;

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        Checks.checkAllPostsBelongToUser(postIds, userDbService::getPostAuthorId, reassignToId,"Пост должен быть переназначен новому автору");
    }

    @Test(description = "TC-USER-07: У каждого поста обновился автор", groups = {"needsCleanup"})
    public void tcUser07_eachPostHasNewAuthor() {

        UserRequest request = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        userIds.add(userId);

        postIds.addAll(postsClient.createPostsForAuthor(3, userId, status, AuthType.ADMIN));

        int reassignToId = 1;

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        Checks.checkPostsAuthorBoth(postIds,
                id -> postsClient.getPostById(id, AuthType.ADMIN),
                userDbService::getPostAuthorId,
                reassignToId,
                "Автор должен обновиться");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-USER-N01: Создание пользователя без пароля")
    @Story("Создание пользователя")
    public void tcUserN01_createUserWithoutPassword() {
        int beforeCount = userDbService.getUsersCount();

        UserRequest request = new UserRequest(username, email, null, null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(response, 400);

        int afterCount = userDbService.getUsersCount();
        Checks.checkEquals(afterCount, beforeCount, "Новый пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N02: Создание пользователя с уже существующим email", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUserN02_createUserDuplicateEmail() {
        UserRequest request1 = new UserRequest(username + "1", email, password, null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        userIds.add(createResponse.jsonPath().getInt("id"));

        int beforeCount = userDbService.getUsersCount();

        UserRequest request2 = new UserRequest(username + "2", email, password, null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        Checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = userDbService.getUsersCount();
        Checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N03: Создание пользователя с уже существующим username", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUserN03_createUserDuplicateUsername() {
        UserRequest request1 = new UserRequest(username, "1" + email, password, null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        userIds.add(createResponse.jsonPath().getInt("id"));

        int beforeCount = userDbService.getUsersCount();

        UserRequest request2 = new UserRequest(username, "2" + email, password, null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        Checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = userDbService.getUsersCount();
        Checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N04: Получение пользователя без авторизации")
    @Story("Авторизация")
    public void tcUserN04_getCurrentUserUnauthorized() {
        Response response = usersClient.getCurrentUserWithoutAuth();
        Checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-USER-N05: Удаление пользователя без указания reassign", groups = {"needsCleanup"})
    @Story("Удаление пользователя")
    public void tcUserN05_deleteUserWithoutReassign() {
        UserRequest request = new UserRequest(username, email, password, null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        userIds.add(id);

        Checks.checkEquals(userDbService.isUserExists(id), true, "Пользователь должен существовать до попытки удаления");

        Response response = usersClient.deleteUserWithoutReassign(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 400);
        Checks.checkEquals(userDbService.isUserExists(id), true, "Пользователь должен остаться в БД");
    }

    @Test(description = "TC-USER-N06: Создание пользователя не-админом")
    @Story("Авторизация")
    public void tcUserN06_createUserNotAdmin() {
        UserRequest request = new UserRequest(username, email, password, null, null);

        Response response = usersClient.createUser(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        Checks.checkStatusCode(response, 403);
    }
}