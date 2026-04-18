package tests.d1;

import api.BaseApi.AuthType;
import checks.Checks;
import client.CategoriesClient;
import db.service.CategoryDbService;
import enums.TestDataTemplates;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.CategoryRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D1 тесты для API рубрик WordPress (endpoint: /wp/v2/categories).
 */
@Epic("WordPress API Тестирование")
@Feature("Categories (Рубрики) - D1 CRUD")
public class CategoriesD1Tests {

    CategoriesClient categoriesClient = new CategoriesClient();
    CategoryDbService categoryDbService = new CategoryDbService();

    private final List<Integer> categoryIds = new ArrayList<>();
    private String categoryName;

    @BeforeMethod
    public void create() {
        categoryName = TestDataTemplates.CATEGORY_NAME.getUniqueValue();
    }

    @AfterMethod(groups = {"needsCleanup"})
    public void cleanUp() {
        categoryDbService.deleteCategoriesHard(categoryIds);
        categoryIds.clear();
    }

    // ========== ПОЗИТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-CAT-01: Создание рубрики с обязательным полем name", groups = {"needsCleanup"})
    @Story("Создание рубрики")
    public void tcCat01_createCategoryMinimalFields() {
        CategoryRequest request = new CategoryRequest(categoryName, null, null);

        Response response = categoriesClient.createCategory(request, AuthType.ADMIN);

        Checks.checkStatusCode(response, 201);
        Checks.checkJsonFieldNotNull(response, "id");
        Checks.checkJsonField(response, "name", request.getName());
        Checks.checkJsonFieldNotNull(response, "slug");

        int id = response.jsonPath().getInt("id");
        categoryIds.add(id);

        Checks.checkTrue(categoryDbService.isCategoryExists(id), "Рубрика должна существовать в БД");
        Checks.checkEquals(categoryDbService.getCategoryName(id), request.getName(), "Имя рубрики в БД");
    }

    @Test(description = "TC-CAT-02: Получение списка рубрик (GET)", groups = {"needsCleanup"})
    @Story("Получение рубрик")
    public void tcCat02_getCategoriesList() {
        CategoryRequest testRequest = new CategoryRequest(categoryName, null, null);
        Response createResponse = categoriesClient.createCategory(testRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);

        int id = createResponse.jsonPath().getInt("id");
        categoryIds.add(id);

        Response response = categoriesClient.getCategories(AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkListNotEmpty(response, "");

        List<Integer> idsFromApi = response.jsonPath().getList("id");

        boolean containsCreated = idsFromApi.contains(id);

        Checks.checkTrue(containsCreated, "Созданная категория должна быть в ответе");
    }

    @Test(description = "TC-CAT-03: Получение рубрики по ID", groups = {"needsCleanup"})
    @Story("Получение рубрик")
    public void tcCat03_getCategoryById() {
        CategoryRequest createRequest = new CategoryRequest(categoryName, null, null);
        Response createResponse = categoriesClient.createCategory(createRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        categoryIds.add(id);

        Response response = categoriesClient.getCategoryById(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "id", id);
        Checks.checkJsonField(response, "name", createRequest.getName());

        Checks.checkEquals(categoryDbService.getCategoryName(id), createRequest.getName(), "Имя рубрики в БД");
    }

    @Test(description = "TC-CAT-04: Обновление имени рубрики", groups = {"needsCleanup"})
    @Story("Обновление рубрики")
    public void tcCat04_updateCategoryName() {
        CategoryRequest createRequest = new CategoryRequest(categoryName, null, null);
        Response createResponse = categoriesClient.createCategory(createRequest, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        categoryIds.add(id);

        CategoryRequest updateRequest = new CategoryRequest("Renamed Cat", null, null);
        Response response = categoriesClient.updateCategory(id, updateRequest, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkJsonField(response, "name", "Renamed Cat");
        Checks.checkJsonField(response, "slug", "renamed-cat");

        Checks.checkEquals(categoryDbService.getCategoryName(id), "Renamed Cat", "Имя рубрики в БД должно обновиться");
    }

    @Test(description = "TC-CAT-05: Удаление рубрики", groups = {"needsCleanup"})
    @Story("Удаление рубрики")
    public void tcCat05_deleteCategory() {
        CategoryRequest request = new CategoryRequest(categoryName, null, null);
        Response createResponse = categoriesClient.createCategory(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        int id = createResponse.jsonPath().getInt("id");
        categoryIds.add(id);

        Checks.checkTrue(categoryDbService.isCategoryExists(id), "Рубрика должна существовать до удаления");

        Response response = categoriesClient.deleteCategory(id, AuthType.ADMIN);

        Checks.checkStatusCode(response, 200);
        Checks.checkDeletedTrue(response);

        Checks.checkFalse(categoryDbService.isCategoryExists(id), "Рубрика не должна существовать после удаления");
    }

    // ========== НЕГАТИВНЫЕ ТЕСТЫ ==========

    @Test(description = "TC-CAT-N01: Создание рубрики без name")
    @Story("Создание рубрики")
    public void tcCatN01_createCategoryWithoutName() {
        int beforeCount = categoryDbService.getCategoriesCount();

        CategoryRequest request = new CategoryRequest(null, null, null);
        Response response = categoriesClient.createCategory(request, AuthType.ADMIN);

        Checks.checkStatusCode(response, 400);

        int afterCount = categoryDbService.getCategoriesCount();
        Checks.checkEquals(afterCount, beforeCount, "Новая рубрика не должна создаться в БД");
    }

    @Test(description = "TC-CAT-N02: Создание рубрики с дубликатом имени", groups = {"needsCleanup"})
    @Story("Создание рубрики")
    public void tcCatN02_createCategoryDuplicateName() {
        CategoryRequest request = new CategoryRequest(categoryName, null, null);
        Response createResponse = categoriesClient.createCategory(request, AuthType.ADMIN);
        Checks.checkStatusCode(createResponse, 201);
        categoryIds.add(createResponse.jsonPath().getInt("id"));

        int beforeCount = categoryDbService.getCategoriesCount();

        Response duplicateResponse = categoriesClient.createCategory(request, AuthType.ADMIN);

        Checks.checkStatusCode(duplicateResponse, 400);

        int afterCount = categoryDbService.getCategoriesCount();
        Checks.checkEquals(afterCount, beforeCount, "Дубликат не должен создаться");
    }

    @Test(description = "TC-CAT-N03: Получение несуществующей рубрики")
    @Story("Получение рубрик")
    public void tcCatN03_getNonExistentCategory() {
        Response response = categoriesClient.getCategoryById(999999, AuthType.ADMIN);
        Checks.checkStatusCode(response, 404);
    }

    @Test(description = "TC-CAT-N04: Создание рубрики без прав")
    @Story("Авторизация")
    public void tcCatN04_createCategoryWithoutRights() {
        CategoryRequest request = new CategoryRequest(categoryName, null, null);

        Response response = categoriesClient.createCategory(request, AuthType.CUSTOM, "subscriber_user", "subscriber_pass");

        Checks.checkStatusCode(response, 403);
    }
}