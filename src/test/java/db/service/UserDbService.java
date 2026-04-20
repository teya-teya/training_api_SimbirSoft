package db.service;

import db.dao.UserDao;
import enums.TestDataTemplates;
import helpers.TestDataGenerator;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_users, wp_posts, wp_usermeta).
 */
@Slf4j
public class UserDbService {

    private final UserDao userDao = new UserDao();

    @Step("Проверка существования пользователя в БД по ID: {id}")
    public boolean isUserExists(int id) {
        return userDao.userExists(id);
    }

    @Step("Получение email пользователя из БД по ID: {id}")
    public String getUserEmail(int id) {
        return userDao.getEmail(id);
    }

    @Step("Получение username пользователя из БД по ID: {id}")
    public String getUserUsername(int id) {
        return userDao.getUsername(id);
    }

    @Step("Получение user_nicename пользователя {userId}")
    public String getUserNicename(int userId) {
        return userDao.getNicename(userId);
    }

    @Step("Получение количества всех пользователей в БД")
    public int getUsersCount() {
        return userDao.countAll();
    }

    @Step("Получение количества пользователей с login LIKE pattern")
    public int getUsersCountByLoginLike(String pattern) {
        return userDao.countByLoginLike(pattern);
    }

    @Step("Создание тестового пользователя в БД")
    public int createTestUser(String login, String email, String password) {
        String slug = TestDataTemplates.toSlug(login);
        return userDao.create(login, email, password, slug);
    }

    @Step("Создание {count} тестовых пользователей ")
    public List<Integer> createTestUsers(int count) {
        List<Integer> userIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String login = TestDataTemplates.USER_USERNAME.getUniqueValue() + "_" + i;
            String email = i + "_" + TestDataGenerator.getRandomEmail();
            String password = TestDataGenerator.getRandomPassword();
            int userId = createTestUser(login, email, password);
            userIds.add(userId);
        }
        return userIds;
    }

    @Step("Создание тестового пользователя с nicename")
    public int createTestUserWithNicename(String login, String email, String password, String nicename) {
        return userDao.createWithNicename(login, email, password, nicename);
    }

    @Step("Получение автора поста {postId}")
    public int getPostAuthorId(int postId) {
        return userDao.getPostAuthorId(postId);
    }

    @Step("Принудительное удаление пользователя из БД по ID: {id}")
    public void deleteUserHard(int id) {
        userDao.delete(id);
    }

    @Step("Удаление списка пользователей из БД по ID")
    public void deleteUsersHard(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        for (int userId : userIds) {
            try {
                deleteUserHard(userId);
            } catch (Exception e) {
                log.warn("Не удалось удалить пользователя {}", userId);
            }
        }
    }
}