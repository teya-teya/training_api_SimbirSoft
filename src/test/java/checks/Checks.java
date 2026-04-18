package checks;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;
import java.util.function.IntFunction;

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
    public static void checkStatusCode(Response response, int expectedStatus) {
        Assert.assertEquals(response.statusCode(), expectedStatus,
                "Статус код не совпадает. Тело ответа:\n" + response.asPrettyString());
    }

    @Step("Проверка, что поле {jsonPath} равно {expectedValue}")
    public static void checkJsonField(Response response, String jsonPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(jsonPath);
        Assert.assertEquals(actualValue, expectedValue,
                "Поле '" + jsonPath + "' не совпадает. Ожидалось: " + expectedValue + ", получено: " + actualValue);
    }

    @Step("Проверка, что поле {jsonPath} не null")
    public static void checkJsonFieldNotNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        Assert.assertNotNull(value, "Поле '" + jsonPath + "' равно null");
    }

    @Step("Проверка, что поле {jsonPath} пустое или null")
    public static void checkJsonFieldNullOrEmpty(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        boolean isEmpty = value == null || (value instanceof String && ((String) value).isEmpty());
        Assert.assertTrue(isEmpty, "Поле '" + jsonPath + "' должно быть null или пустым, но получено: " + value);
    }

    @Step("Проверка, что список не пустой")
    public static void checkListNotEmpty(Response response, String jsonPath) {
        int size = response.jsonPath().getList(jsonPath).size();
        Assert.assertTrue(size > 0, "Список по пути '" + jsonPath + "' пуст");
    }

    @Step("Проверка, что deleted = true")
    public static void checkDeletedTrue(Response response) {
        boolean deleted = response.jsonPath().getBoolean("deleted");
        Assert.assertTrue(deleted, "Поле 'deleted' не равно true. Фактическое значение: " + deleted);
    }

    // ========== УНИВЕРСАЛЬНЫЕ ПРОВЕРКИ (для БД и любых объектов) ==========

    @Step("Проверка, что значение '{actual}' равно '{expected}' (описание: {description})")
    public static void checkEquals(Object actual, Object expected, String description) {
        Assert.assertEquals(actual, expected,
                "Ошибка проверки: " + description + ". Ожидалось: " + expected + ", получено: " + actual);
    }

    @Step("Проверка, что условие истинно: {description}")
    public static void checkTrue(boolean condition, String description) {
        Assert.assertTrue(condition, "Условие не выполнено: " + description);
    }

    @Step("Проверка, что условие ложно: {description}")
    public static void checkFalse(boolean condition, String description) {
        Assert.assertFalse(condition, "Условие должно быть ложным: " + description);
    }

    @Step("Проверка, что все элементы списка соответствуют ожидаемому значению")
    public static <T> void checkAllMatch(List<T> actualValues, T expectedValue, String message) {
        boolean allMatch = actualValues.stream().allMatch(value -> value.equals(expectedValue));
        checkTrue(allMatch, message);
    }

    @Step("Проверка, что список содержит все ожидаемые элементы")
    public static <T> void checkListContainsAll(List<T> expectedItems, List<T> actualItems, String messagePrefix) {
        for (T item : expectedItems) {
            checkTrue(actualItems.contains(item), messagePrefix + item);
        }
    }

    @Step("Проверка, что ни один пост из списка не принадлежит пользователю {userId}")
    public static void checkNoPostBelongsToUser(List<Integer> postIds, IntFunction<Integer> authorExtractor, int userId, String message) {
        for (int postId : postIds) {
            int authorId = authorExtractor.apply(postId);
            checkTrue(authorId != userId, message + " Post ID: " + postId);
        }
    }

    @Step("Проверка, что все посты из списка принадлежат пользователю {expectedAuthorId}")
    public static void checkAllPostsBelongToUser(List<Integer> postIds,  IntFunction<Integer> authorExtractor, int expectedAuthorId, String message) {
        for (int postId : postIds) {
            int actualAuthorId = authorExtractor.apply(postId);
            checkEquals(actualAuthorId, expectedAuthorId, message + " Post ID: " + postId);
        }
    }

    @Step("Проверка, что все посты из списка имеют автора {expectedAuthorId} (API + БД)")
    public static void checkPostsAuthorBoth(List<Integer> postIds,
                                            IntFunction<Response> apiGetter,
                                            IntFunction<Integer> dbExtractor,
                                            int expectedAuthorId,
                                            String message) {
        for (int postId : postIds) {
            Response response = apiGetter.apply(postId);
            checkStatusCode(response, 200);
            checkEquals(response.jsonPath().getInt("author"), expectedAuthorId, message + " (API) Post ID: " + postId);
            checkEquals(dbExtractor.apply(postId), expectedAuthorId, message + " (DB) Post ID: " + postId);
        }
    }
}