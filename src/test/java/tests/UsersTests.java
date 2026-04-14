package tests;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import client.UsersClient;
import db.UserDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.UserRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;

/**
 * Набор тестов для API пользователей WordPress (endpoint: /wp/v2/users).
 */
@Epic("WordPress API Тестирование")
@Feature("Users (Пользователи)")
public class UsersTests {

    UsersClient usersClient = new UsersClient();
    PostsClient postsClient = new PostsClient();
    UserDbService dbService = new UserDbService();
    Checks checks = new Checks();

    private int createdUserId = -1;

    @BeforeMethod
    public void setUp() {
        createdUserId = -1;
    }

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() throws SQLException {
        dbService.deleteUserHard(createdUserId);
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-USER-01: Создание нового пользователя (минимальные поля)", groups = {"needsCleanup"})
    @Story("Создание пользователя")
    public void tcUser01_createUserMinimalFields() throws SQLException {
        UserRequest request = new UserRequest("testuser", "test@example.com", "Test123!", null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonField(response, "username", request.getUsername());
        checks.checkJsonField(response, "email", request.getEmail());

        int id = response.jsonPath().getInt("id");
        createdUserId = id;

        checks.checkEquals(dbService.isUserExists(id), true, "Пользователь должен существовать в БД");
        checks.checkEquals(dbService.getUserUsername(id), request.getUsername(), "Username в БД");
        checks.checkEquals(dbService.getUserEmail(id), request.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-02: Получение другого пользователя по ID", groups = {"needsCleanup"})
    @Story("Получение пользователей")
    public void tcUser02_getUserById() throws SQLException {
        UserRequest createRequest = new UserRequest("testuser_get", "testget@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdUserId = id;

        Response response = usersClient.getUserById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);

        checks.checkEquals(dbService.getUserEmail(id), createRequest.getEmail(), "Email в БД");
    }

    @Test(description = "TC-USER-03: Обновление email пользователя", groups = {"needsCleanup"})
    @Story("Обновление пользователя")
    public void tcUser03_updateUserEmail() throws SQLException {
        UserRequest createRequest = new UserRequest("testuser_update", "old@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdUserId = id;

        String body = "{\"email\": \"newemail@example.com\"}";
        Response response = usersClient.updateUserRaw(id, body, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "email", "newemail@example.com");

        checks.checkEquals(dbService.getUserEmail(id), "newemail@example.com", "Email в БД");
    }

    @Test(description = "TC-USER-04: Удаление пользователя с reassign", groups = {"needsCleanup"})
    public void tcUser04_deleteUserWithReassign() throws SQLException {
        UserRequest request = new UserRequest("usertodelete", "delete@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdUserId = id;

        Response response = usersClient.deleteUser(id, 1, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);
        checks.checkFalse(dbService.isUserExists(id), "Пользователь должен быть удален");
    }

    @Test(description = "TC-USER-05: У удаленного пользователя нет постов", groups = {"needsCleanup"})
    public void tcUser05_postsRemovedFromDeletedUser() throws SQLException {
        UserRequest request = new UserRequest("userposts", "posts@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        createdUserId = userId;

        postsClient.createPostsForUser(userId, 3);

        usersClient.deleteUser(userId, 1, AuthType.ADMIN);

        checks.checkEquals(dbService.getPostsCountByAuthor(userId), 0, "У удаленного пользователя не должно быть постов");
    }

    @Test(description = "TC-USER-06: Посты переназначены новому автору", groups = {"needsCleanup"})
    public void tcUser06_postsReassigned() throws SQLException {
        UserRequest request = new UserRequest("userreassign", "reassign@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        createdUserId = userId;

        int postsCount = 3;
        postsClient.createPostsForUser(userId, postsCount);

        int reassignToId = 1;
        int before = dbService.getPostsCountByAuthor(reassignToId);

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        int after = dbService.getPostsCountByAuthor(reassignToId);

        checks.checkEquals(after, before + postsCount, "Посты должны перейти новому автору");
    }

    @Test(description = "TC-USER-07: У каждого поста обновился автор", groups = {"needsCleanup"})
    public void tcUser07_eachPostHasNewAuthor() throws SQLException {
        UserRequest request = new UserRequest("usercheckposts", "check@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int userId = createResponse.jsonPath().getInt("id");
        createdUserId = userId;

        List<Integer> postIds = postsClient.createPostsForUser(userId, 3);

        int reassignToId = 1;

        usersClient.deleteUser(userId, reassignToId, AuthType.ADMIN);

        for (int postId : postIds) {
            checks.checkEquals(dbService.getPostAuthorId(postId), reassignToId,
                    "Пост " + postId + " должен принадлежать новому автору");
        }
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-USER-N01: Создание пользователя без пароля")
    @Story("Создание пользователя")
    public void tcUserN01_createUserWithoutPassword() throws SQLException {
        int beforeCount = dbService.getUsersCount();

        UserRequest request = new UserRequest("nopass", "nopass@ex.com", null, null, null);

        Response response = usersClient.createUser(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 400);

        int afterCount = dbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Новый пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N02: Создание пользователя с уже существующим email")
    @Story("Создание пользователя")
    public void tcUserN02_createUserDuplicateEmail() throws SQLException {
        UserRequest request1 = new UserRequest("user1", "duplicate@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdUserId = createResponse.jsonPath().getInt("id");

        int beforeCount = dbService.getUsersCount();

        UserRequest request2 = new UserRequest("user2", "duplicate@example.com", "Test123!", null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = dbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N03: Создание пользователя с уже существующим username")
    @Story("Создание пользователя")
    public void tcUserN03_createUserDuplicateUsername() throws SQLException {
        UserRequest request1 = new UserRequest("duplicate_user", "first@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request1, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdUserId = createResponse.jsonPath().getInt("id");

        int beforeCount = dbService.getUsersCount();

        UserRequest request2 = new UserRequest("duplicate_user", "second@example.com", "Test123!", null, null);

        Response duplicateResponse = usersClient.createUser(request2, AuthType.ADMIN);

        checks.checkStatusCode(duplicateResponse, 409);

        int afterCount = dbService.getUsersCount();
        checks.checkEquals(afterCount, beforeCount, "Второй пользователь не должен создаться в БД");
    }

    @Test(description = "TC-USER-N04: Получение пользователя без авторизации")
    @Story("Авторизация")
    public void tcUserN04_getCurrentUserUnauthorized() {
        Response response = usersClient.getCurrentUserWithoutAuth();
        checks.checkStatusCode(response, 401);
    }

    @Test(description = "TC-USER-N05: Удаление пользователя без указания reassign")
    @Story("Удаление пользователя")
    public void tcUserN05_deleteUserWithoutReassign() throws SQLException {
        UserRequest request = new UserRequest("user_noreassign", "noreassign@example.com", "Test123!", null, null);

        Response createResponse = usersClient.createUser(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        createdUserId = id;

        checks.checkEquals(dbService.isUserExists(id), true, "Пользователь должен существовать до попытки удаления");

        Response response = usersClient.deleteUserWithoutReassign(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 400);
        checks.checkEquals(dbService.isUserExists(id), true, "Пользователь должен остаться в БД");
    }

    @Test(description = "TC-USER-N06: Создание пользователя не-админом")
    @Story("Авторизация")
    public void tcUserN06_createUserNotAdmin() {
        UserRequest request = new UserRequest("nonadmin_user", "nonadmin@example.com", "Test123!", null, null);

        Response response = usersClient.createUser(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        checks.checkStatusCode(response, 403);
    }
}