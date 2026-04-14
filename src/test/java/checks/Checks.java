package checks;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.testng.Assert;

/**
 * Универсальный класс для проверок API и БД.
 * <p>
 * Содержит методы для:
 * <ul>
 *     <li>Проверки статус кодов и полей JSON-ответов</li>
 *     <li>Проверки значений из базы данных</li>
 *     <li>Условных проверок (true/false)</li>
 * </ul>
 * <p>
 */
public class Checks {

    // ========== API ПРОВЕРКИ ==========

    @Step("Проверка статус кода: ожидается {expectedStatus}")
    public void checkStatusCode(Response response, int expectedStatus) {
        Assert.assertEquals(response.statusCode(), expectedStatus,
                "Статус код не совпадает. Тело ответа:\n" + response.asPrettyString());
    }

    @Step("Проверка, что поле {jsonPath} равно {expectedValue}")
    public void checkJsonField(Response response, String jsonPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(jsonPath);
        Assert.assertEquals(actualValue, expectedValue,
                "Поле '" + jsonPath + "' не совпадает. Ожидалось: " + expectedValue + ", получено: " + actualValue);
    }

    @Step("Проверка, что поле {jsonPath} не null")
    public void checkJsonFieldNotNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        Assert.assertNotNull(value, "Поле '" + jsonPath + "' равно null");
    }

    @Step("Проверка, что поле {jsonPath} пустое или null")
    public void checkJsonFieldNullOrEmpty(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        boolean isEmpty = value == null || (value instanceof String && ((String) value).isEmpty());
        Assert.assertTrue(isEmpty, "Поле '" + jsonPath + "' должно быть null или пустым, но получено: " + value);
    }

    @Step("Проверка, что список не пустой")
    public void checkListNotEmpty(Response response, String jsonPath) {
        int size = response.jsonPath().getList(jsonPath).size();
        Assert.assertTrue(size > 0, "Список по пути '" + jsonPath + "' пуст");
    }

    @Step("Проверка, что deleted = true")
    public void checkDeletedTrue(Response response) {
        boolean deleted = response.jsonPath().getBoolean("deleted");
        Assert.assertTrue(deleted, "Поле 'deleted' не равно true. Фактическое значение: " + deleted);
    }

    // ========== УНИВЕРСАЛЬНЫЕ ПРОВЕРКИ (для БД и любых объектов) ==========

    @Step("Проверка, что значение '{actual}' равно '{expected}' (описание: {description})")
    public void checkEquals(Object actual, Object expected, String description) {
        Assert.assertEquals(actual, expected,
                "Ошибка проверки: " + description + ". Ожидалось: " + expected + ", получено: " + actual);
    }

    @Step("Проверка, что условие истинно: {description}")
    public void checkTrue(boolean condition, String description) {
        Assert.assertTrue(condition, "Условие не выполнено: " + description);
    }

    @Step("Проверка, что условие ложно: {description}")
    public void checkFalse(boolean condition, String description) {
        Assert.assertFalse(condition, "Условие должно быть ложным: " + description);
    }
}