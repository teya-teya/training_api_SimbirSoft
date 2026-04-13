package tests;

import api.BaseApi.AuthType;
import checks.Checks;
import client.CategoriesClient;
import db.CategoryDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.CategoryRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;

/**
 * Набор тестов для API рубрик WordPress (endpoint: /wp/v2/categories).
 */
@Epic("WordPress API Тестирование")
@Feature("Categories (Рубрики)")
public class CategoriesTests {

    CategoriesClient client = new CategoriesClient();
    CategoryDbService db = new CategoryDbService();
    Checks checks = new Checks();

    private int createdId = -1;

    @BeforeMethod
    public void setUp() {
        createdId = -1;
    }

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() throws SQLException {
        if (createdId != -1 && db.isCategoryExists(createdId)) {
            db.deleteCategoryHard(createdId);
        }
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-CAT-01: Создание рубрики с обязательным полем name", groups = {"needsCleanup"})
    @Story("Создание рубрики")
    public void tcCat01_createCategoryMinimalFields() throws SQLException {
        CategoryRequest request = new CategoryRequest("Test Category", null, null);

        Response response = client.createCategory(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 201);
        checks.checkJsonFieldNotNull(response, "id");
        checks.checkJsonField(response, "name", request.getName());
        checks.checkJsonFieldNotNull(response, "slug");

        int id = response.jsonPath().getInt("id");
        createdId = id;

        checks.checkTrue(db.isCategoryExists(id), "Рубрика должна существовать в БД");
        checks.checkEquals(db.getCategoryName(id), request.getName(), "Имя рубрики в БД");
    }

    @Test(description = "TC-CAT-02: Получение списка рубрик (GET)", groups = {"needsCleanup"})
    @Story("Получение рубрик")
    public void tcCat02_getCategoriesList() throws SQLException {
        CategoryRequest testRequest = new CategoryRequest("List Test Category", null, null);
        Response createResponse = client.createCategory(testRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdId = createResponse.jsonPath().getInt("id");

        Response response = client.getCategories(AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkListNotEmpty(response, "");

        int responseCount = response.jsonPath().getList("").size();
        int dbCount = db.getCategoriesCount();

        checks.checkEquals(responseCount, dbCount, "Количество рубрик в ответе и БД");
    }

    @Test(description = "TC-CAT-03: Получение рубрики по ID", groups = {"needsCleanup"})
    @Story("Получение рубрик")
    public void tcCat03_getCategoryById() throws SQLException {
        CategoryRequest createRequest = new CategoryRequest("Test Category for Get", null, null);
        Response createResponse = client.createCategory(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdId = id;

        Response response = client.getCategoryById(id, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "id", id);
        checks.checkJsonField(response, "name", createRequest.getName());

        checks.checkEquals(db.getCategoryName(id), createRequest.getName(), "Имя рубрики в БД");
    }

    @Test(description = "TC-CAT-04: Обновление имени рубрики", groups = {"needsCleanup"})
    @Story("Обновление рубрики")
    public void tcCat04_updateCategoryName() throws SQLException {
        CategoryRequest createRequest = new CategoryRequest("Original Name", null, null);
        Response createResponse = client.createCategory(createRequest, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        createdId = id;

        CategoryRequest updateRequest = new CategoryRequest("Renamed Cat", null, null);
        Response response = client.updateCategory(id, updateRequest, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkJsonField(response, "name", "Renamed Cat");
        checks.checkJsonField(response, "slug", "renamed-cat");

        checks.checkEquals(db.getCategoryName(id), "Renamed Cat", "Имя рубрики в БД должно обновиться");
    }

    @Test(description = "TC-CAT-05: Удаление рубрики")
    @Story("Удаление рубрики")
    public void tcCat05_deleteCategory() throws SQLException {
        CategoryRequest request = new CategoryRequest("Category to Delete", null, null);
        Response createResponse = client.createCategory(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdId = createResponse.jsonPath().getInt("id");

        checks.checkTrue(db.isCategoryExists(createdId), "Рубрика должна существовать до удаления");

        Response response = client.deleteCategory(createdId, AuthType.ADMIN);

        checks.checkStatusCode(response, 200);
        checks.checkDeletedTrue(response);

        checks.checkFalse(db.isCategoryExists(createdId), "Рубрика не должна существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-CAT-N01: Создание рубрики без name")
    @Story("Создание рубрики")
    public void tcCatN01_createCategoryWithoutName() throws SQLException {
        int beforeCount = db.getCategoriesCount();

        CategoryRequest request = new CategoryRequest(null, null, null);
        Response response = client.createCategory(request, AuthType.ADMIN);

        checks.checkStatusCode(response, 400);

        int afterCount = db.getCategoriesCount();
        checks.checkEquals(afterCount, beforeCount, "Новая рубрика не должна создаться в БД");
    }

    @Test(description = "TC-CAT-N02: Создание рубрики с дубликатом имени")
    @Story("Создание рубрики")
    public void tcCatN02_createCategoryDuplicateName() throws SQLException {
        CategoryRequest request = new CategoryRequest("Duplicate", null, null);
        Response createResponse = client.createCategory(request, AuthType.ADMIN);
        checks.checkStatusCode(createResponse, 201);
        createdId = createResponse.jsonPath().getInt("id");

        int beforeCount = db.getCategoriesCount();

        Response duplicateResponse = client.createCategory(request, AuthType.ADMIN);

        checks.checkStatusCode(duplicateResponse, 400);

        int afterCount = db.getCategoriesCount();
        checks.checkEquals(afterCount, beforeCount, "Дубликат не должен создаться");
    }

    @Test(description = "TC-CAT-N03: Получение несуществующей рубрики")
    @Story("Получение рубрик")
    public void tcCatN03_getNonExistentCategory() {
        Response response = client.getCategoryById(999999, AuthType.ADMIN);
        checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-CAT-N04: Создание рубрики без прав")
    @Story("Авторизация")
    public void tcCatN04_createCategoryWithoutRights() {
        CategoryRequest request = new CategoryRequest("No Rights Category", null, null);

        Response response = client.createCategory(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        checks.checkStatusCode(response, 403);
    }
}