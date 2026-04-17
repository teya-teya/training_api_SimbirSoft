package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.CategoriesClient;
import db.service.CategoryDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
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
    Checks checks = new Checks();

    private final List<Integer> categoryIds = new ArrayList<>();

    @AfterMethod
    public void cleanUp() {
        categoryDbService.deleteCategoriesHard(categoryIds);
        categoryIds.clear();
    }

    @Test(description = "TC-CAT-01-D2: Получение списка тестовых рубрик")
    public void tcCat01D2_getCategoriesList() {

        for (int i = 0; i < 3; i++) {
            int catId = categoryDbService.createTestCategory("autotest_category_" + i, "autotest-category-" + i);
            categoryIds.add(catId);
        }

        Response response = categoriesClient.getCategories(AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        for (int id : categoryIds) {
            checks.checkTrue(ids.contains(id), "Ответ должен содержать категорию " + id);
        }

        int dbCount = categoryDbService.getCategoriesCountByNameLike("autotest_category_%");

        long apiCount = response.jsonPath().getList("name", String.class)
                .stream()
                .filter(n -> n != null && n.startsWith("autotest_category_"))
                .count();

        checks.checkEquals((int) apiCount, dbCount, "Количество тестовых категорий должно совпадать");
    }

    @Test(description = "TC-CAT-02-D2: Получение рубрики по ID")
    public void tcCat02D2_getCategoryById() {
        int categoryId = categoryDbService.createTestCategory("autotest_category", "autotest-category");
        categoryIds.add(categoryId);

        Response response = categoriesClient.getCategoryById(categoryId, AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        checks.checkEquals(response.jsonPath().getString("name"), "autotest_category", "name должен совпадать");
        checks.checkEquals(response.jsonPath().getString("slug"), "autotest-category", "slug должен совпадать");

        checks.checkEquals(categoryDbService.getCategoryName(categoryId), "autotest_category", "Название в БД должно совпадать");
        checks.checkEquals(categoryDbService.getCategorySlug(categoryId), "autotest-category", "Slug в БД должен совпадать");
    }
}