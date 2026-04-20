package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.UsersClient;
import db.service.UserDbService;
import enums.TestDataTemplates;
import helpers.TestDataGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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

    private final List<Integer> userIds = new ArrayList<>();
    private String username, email, password, slug;

    @BeforeMethod
    public void create() {
        username = TestDataTemplates.USER_USERNAME.getUniqueValue();
        email = TestDataGenerator.getRandomEmail();
        password = TestDataGenerator.getRandomPassword();
        slug = TestDataTemplates.toSlug(username);
    }

    @AfterMethod
    public void cleanUp() {
        userDbService.deleteUsersHard(userIds);
        userIds.clear();
    }

    @Test(description = "TC-USER-01-D2: Получение списка пользователей")
    public void tcUser01D2_getUsersList() {

        userIds.addAll(userDbService.createTestUsers(3));

        Response response = usersClient.getUsers(AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        Checks.checkListContainsAll(userIds, ids, "Ответ должен содержать пользователя ");

        int dbCount = userDbService.getUsersCountByLoginLike(TestDataTemplates.USER_USERNAME.getValue() + "%");
        Checks.checkTrue(dbCount >= 3, "В БД должно быть минимум 3 пользователя");
    }

    @Test(description = "TC-USER-02-D2: Получение пользователя по ID")
    public void tcUser02D2_getUserById() {
        int userId = userDbService.createTestUser(username, email, password);
        userIds.add(userId);

        Response response = usersClient.getUserById(userId, AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        Checks.checkEquals(response.jsonPath().getInt("id"), userId, "ID должен совпадать");
        Checks.checkEquals(response.jsonPath().getString("slug"), slug, "slug должен совпадать");

        Checks.checkEquals(userDbService.getUserNicename(userId), slug, "slug в БД должен совпадать");

        Checks.checkEquals(userDbService.getUserUsername(userId), username, "Логин в БД должен совпадать");
        Checks.checkEquals(userDbService.getUserEmail(userId), email, "Email в БД должен совпадать");
    }

    @Test(description = "TC-USER-03-D2: Получение пользователя с фильтром по slug")
    public void tcUser03D2_filterBySlug() {
        int userId = userDbService.createTestUserWithNicename(username, email, password, slug);
        userIds.add(userId);

        Response response = usersClient.getUsersBySlug(slug, AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        List<Object> users = response.jsonPath().getList("");
        Checks.checkEquals(users.size(), 1, "Должен вернуться 1 пользователь");

        Checks.checkEquals(response.jsonPath().getString("[0].slug"), slug, "slug должен совпадать");

        Checks.checkEquals(userDbService.getUserNicename(userId), slug, "user_nicename в БД должен совпадать");
    }
}