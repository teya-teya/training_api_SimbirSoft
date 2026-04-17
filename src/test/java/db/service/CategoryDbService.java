package db.service;

import db.dao.CategoryDao;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_terms, wp_term_taxonomy).
 */
@Slf4j
public class CategoryDbService {

    private final CategoryDao categoryDao = new CategoryDao();

    @Step("Проверка существования рубрики в БД по ID: {id}")
    public boolean isCategoryExists(int id) {
        return categoryDao.exists(id);
    }

    @Step("Получение имени рубрики из БД по ID: {id}")
    public String getCategoryName(int id) {
        return categoryDao.getName(id);
    }

    @Step("Получение slug рубрики из БД по ID: {id}")
    public String getCategorySlug(int id) {
        return categoryDao.getSlug(id);
    }

    @Step("Получение количества всех рубрик в БД")
    public int getCategoriesCount() {
        return categoryDao.countAll();
    }

    @Step("Создание тестовой рубрики в БД")
    public int createTestCategory(String name, String slug) {
        return categoryDao.create(name, slug);
    }

    @Step("Получение количества рубрик с name LIKE {pattern}")
    public int getCategoriesCountByNameLike(String pattern) {
        return categoryDao.countByNameLike(pattern);
    }

    @Step("Принудительное удаление рубрики из БД по ID: {id}")
    public void deleteCategoryHard(int id) {
        categoryDao.delete(id);
    }

    @Step("Удаление списка рубрик из БД по ID")
    public void deleteCategoriesHard(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return;

        for (int categoryId : categoryIds) {
            try {
                deleteCategoryHard(categoryId);
            } catch (Exception e) {
                log.warn("Не удалось удалить рубрику {}", categoryId);
            }
        }
    }
}