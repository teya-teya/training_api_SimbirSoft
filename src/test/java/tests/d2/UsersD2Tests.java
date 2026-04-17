package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.UsersClient;
import db.service.UserDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D2 тесты для API пользователей WordPress (endpoint: /wp/v2/users).
 */
@Epic("WordPress API Тестирование")
@Feature("Users (Пользователи) - D2 GET запросы")
public class UsersD2Tests {

    UsersClient usersClient = new UsersClient();
    UserDbService userDbService = new UserDbService();
    Checks checks = new Checks();

    private final List<Integer> userIds = new ArrayList<>();

    @AfterMethod
    public void cleanUp() {
        userDbService.deleteUsersHard(userIds);
        userIds.clear();
    }

    @Test(description = "TC-USER-01-D2: Получение списка пользователей")
    public void tcUser01D2_getUsersList() {

        for (int i = 0; i < 3; i++) {
            int id = userDbService.createTestUser("autotest_user_" + i, "test" + i + "@mail.com");
            userIds.add(id);
        }

        Response response = usersClient.getUsers(AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        for (int id : userIds) {
            checks.checkTrue(ids.contains(id), "Ответ содержит пользователя " + id);
        }

        int dbCount = userDbService.getUsersCountByLoginLike("autotest_user_%");
        checks.checkTrue(dbCount >= 3, "В БД должно быть минимум 3 пользователя");
    }

    @Test(description = "TC-USER-02-D2: Получение пользователя по ID")
    public void tcUser02D2_getUserById() {
        int userId = userDbService.createTestUser("autotest_user", "autotest@test.com");
        userIds.add(userId);

        Response response = usersClient.getUserById(userId, AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        checks.checkEquals(response.jsonPath().getInt("id"), userId, "ID должен совпадать");
        checks.checkEquals(response.jsonPath().getString("slug"), "autotest_user", "slug должен совпадать");

        checks.checkEquals(userDbService.getUserUsername(userId), "autotest_user", "Логин в БД должен совпадать");
        checks.checkEquals(userDbService.getUserEmail(userId), "autotest@test.com", "Email в БД должен совпадать");
    }

    @Test(description = "TC-USER-03-D2: Получение пользователя с фильтром по slug")
    public void tcUser03D2_filterBySlug() {
        int userId = userDbService.createTestUserWithNicename("autotest_user", "autotest@test.com", "autotest-user");
        userIds.add(userId);

        Response response = usersClient.getUsersBySlug("autotest-user", AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<Object> users = response.jsonPath().getList("");
        checks.checkEquals(users.size(), 1, "Должен вернуться 1 пользователь");

        checks.checkEquals(response.jsonPath().getString("[0].slug"), "autotest-user", "slug должен совпадать");

        checks.checkEquals(userDbService.getUserNicename(userId), "autotest-user", "user_nicename в БД должен совпадать");
    }
}