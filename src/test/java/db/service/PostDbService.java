package db.service;

import db.dao.PostDao;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Сервис для работы с базой данных WordPress (таблица wp_posts).
 */
@Slf4j
public class PostDbService {

    private final PostDao postDao = new PostDao();

    @Step("Проверка существования поста с ID: {id}")
    public boolean isPostExists(int id) {
        return postDao.exists(id);
    }

    @Step("Получение title поста с ID: {id}")
    public String getPostTitle(int id) {
        return postDao.getField(id, "post_title");
    }

    @Step("Получение content поста с ID: {id}")
    public String getPostContent(int id) {
        return postDao.getField(id, "post_content");
    }

    @Step("Получение статуса поста с ID: {id}")
    public String getPostStatus(int id) {
        return postDao.getField(id, "post_status");
    }

    @Step("Получение типа поста с ID: {id}")
    public String getPostType(int id) {
        return postDao.getField(id, "post_type");
    }

    @Step("Получение количества постов")
    public int getPostsCount() {
        return postDao.countAll();
    }

    @Step("Создание тестового поста")
    public int createTestPost(String title, String content, String status) {
        return postDao.create(title, content, status);
    }

    @Step("Количество постов с title LIKE {pattern}")
    public int getPostsCountByTitleLike(String pattern) {
        return postDao.countByTitleLike(pattern);
    }

    @Step("Количество постов со статусом {status} и title LIKE {pattern}")
    public int getPostsCountByStatusAndTitleLike(String status, String pattern) {
        return postDao.countByStatusAndTitleLike(status, pattern);
    }

    @Step("Количество постов автора {authorId} с title LIKE {pattern}")
    public int getPostsCountByAuthorAndTitleLike(int authorId, String pattern) {
        return postDao.countByAuthorAndTitleLike(authorId, pattern);
    }

    @Step("Создание поста для автора {authorId}")
    public int createTestPostForAuthor(String title, String content, String status, int authorId) {
        return postDao.createForAuthor(title, content, status, authorId);
    }

    @Step("Удаление поста из БД по ID: {id}")
    public void deletePostHard(int id) {
        postDao.delete(id);
    }

    @Step("Удаление списка постов из БД по ID")
    public void deletePostsHard(List<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) return;

        for (int postId : postIds) {
            try {
                deletePostHard(postId);
            } catch (Exception e) {
                log.warn("Не удалось удалить пост {}", postId);
            }
        }
    }
}