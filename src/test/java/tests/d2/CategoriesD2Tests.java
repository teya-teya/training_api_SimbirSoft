package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.CategoriesClient;
import db.service.CategoryDbService;
import enums.TestDataTemplates;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D2 тесты для API рубрик WordPress (endpoint: /wp/v2/categories).
 */
@Epic("WordPress API Тестирование")
@Feature("Categories (Рубрики) - D2 GET запросы")
public class CategoriesD2Tests {

    CategoriesClient categoriesClient = new CategoriesClient();
    CategoryDbService categoryDbService = new CategoryDbService();

    private final List<Integer> categoryIds = new ArrayList<>();
    private String categoryName, slug;

    @BeforeMethod
    public void create() {
        categoryName = TestDataTemplates.CATEGORY_NAME.getUniqueValue();
        slug = TestDataTemplates.toSlug(categoryName);
    }

    @AfterMethod
    public void cleanUp() {
        categoryDbService.deleteCategoriesHard(categoryIds);
        categoryIds.clear();
    }

    @Test(description = "TC-CAT-01-D2: Получение списка тестовых рубрик")
    public void tcCat01D2_getCategoriesList() {
        int countCategory = 3;
        categoryIds.addAll(categoryDbService.createTestCategories(countCategory));

        Response response = categoriesClient.getCategories(AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        Checks.checkListContainsAll(categoryIds, ids, "Ответ должен содержать категорию ");
        Checks.checkTrue(ids.size() >= countCategory,
                String.format("В ответе должно быть не менее %d категорий, получено: %d", countCategory, ids.size()));

        int dbCount = categoryDbService.getCategoriesCountByNameLike("%" + TestDataTemplates.CATEGORY_NAME.getValue() + "%");

        Checks.checkTrue(dbCount >= countCategory,  String.format("В БД должно быть не менее %d тестовых категорий", countCategory));
    }

    @Test(description = "TC-CAT-02-D2: Получение рубрики по ID")
    public void tcCat02D2_getCategoryById() {
        int categoryId = categoryDbService.createTestCategory(categoryName, slug);
        categoryIds.add(categoryId);

        Response response = categoriesClient.getCategoryById(categoryId, AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        Checks.checkEquals(response.jsonPath().getString("name"), categoryName, "name должен совпадать");
        Checks.checkEquals(response.jsonPath().getString("slug"), slug, "slug должен совпадать");

        Checks.checkEquals(categoryDbService.getCategoryName(categoryId), categoryName, "Название в БД должно совпадать");
        Checks.checkEquals(categoryDbService.getCategorySlug(categoryId), slug, "Slug в БД должен совпадать");
    }
}