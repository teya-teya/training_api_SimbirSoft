package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import db.service.PostDbService;
import db.service.UserDbService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * D2 тесты для API записей WordPress (endpoint: /wp/v2/posts).
 */
@Epic("WordPress API Тестирование")
@Feature("Posts (Записи) - D2 GET запросы")
public class PostsD2Tests {

    PostsClient postsClient = new PostsClient();
    PostDbService postDbService = new PostDbService();
    UserDbService userDbService = new UserDbService();
    Checks checks = new Checks();

    private final List<Integer> postIds = new ArrayList<>();

    @AfterMethod
    public void cleanUp() {
        postDbService.deletePostsHard(postIds);
        postIds.clear();
    }

    @Test(description = "TC-POST-01-D2: Получение списка записей")
    public void tcPost01D2_getPostsList() {

        for (int i = 0; i < 3; i++) {
            int postId = postDbService.createTestPost("autotest_post_" + i, "Content " + i, "publish");
            postIds.add(postId);
        }

        Response response = postsClient.getPosts(AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        for (int id : postIds) {
            checks.checkTrue(ids.contains(id), "Ответ должен содержать пост " + id);
        }

        int dbCount = postDbService.getPostsCountByTitleLike("autotest_post_%");
        checks.checkTrue(dbCount >= 3, "В БД должно быть минимум 3 тестовых поста");
    }

    @Test(description = "TC-POST-02-D2: Получение записи по ID")
    public void tcPost02D2_getPostById() {

        int postId = postDbService.createTestPost("autotest_post", "autotest_content", "publish");
        postIds.add(postId);

        Response response = postsClient.getPostById(postId, AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        checks.checkEquals(response.jsonPath().getString("title.rendered"), "autotest_post", "title должен совпадать");
        checks.checkEquals(response.jsonPath().getString("content.rendered").contains("autotest_content"), true, "content должен содержать текст");
        checks.checkEquals(response.jsonPath().getString("status"), "publish", "status должен быть publish");

        checks.checkEquals(postDbService.getPostTitle(postId), "autotest_post", "Заголовок в БД");
        checks.checkEquals(postDbService.getPostContent(postId), "autotest_content", "Контент в БД");
        checks.checkEquals(postDbService.getPostStatus(postId), "publish", "Статус в БД");
    }

    @Test(description = "TC-POST-03-D2: Фильтр по статусу (draft)")
    public void tcPost03D2_filterByStatusDraft() {

        for (int i = 0; i < 2; i++) {
            int postId = postDbService.createTestPost("autotest_post_draft_" + i, "Draft", "draft");
            postIds.add(postId);
        }

        postIds.add(postDbService.createTestPost("autotest_post_publish", "Publish", "publish"));

        Response response = postsClient.getPostsByStatus("draft", AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<String> statuses = response.jsonPath().getList("status");

        checks.checkTrue(statuses.stream().allMatch(s -> s.equals("draft")), "Все посты должны быть draft");

        int dbCount = postDbService.getPostsCountByStatusAndTitleLike("draft", "autotest_post_%");
        checks.checkTrue(dbCount >= 2, "В БД должно быть минимум 2 draft поста");
    }

    @Test(description = "TC-POST-04-D2: Фильтр по автору")
    public void tcPost04D2_filterByAuthor() {

        int authorId = userDbService.createTestUser("autotest_author", "author@test.com");

        for (int i = 0; i < 2; i++) {
            int postId = postDbService.createTestPostForAuthor("autotest_post_" + i, "Content", "publish", authorId);
            postIds.add(postId);
        }

        Response response = postsClient.getPostsByAuthor(authorId, AuthType.ADMIN);
        checks.checkStatusCode(response, 200);

        List<Integer> authors = response.jsonPath().getList("author");

        checks.checkTrue(authors.stream().allMatch(a -> a == authorId), "Все посты принадлежат автору");

        int dbCount = postDbService.getPostsCountByAuthorAndTitleLike(authorId, "autotest_post_%");
        checks.checkTrue(dbCount >= 2, "В БД должно быть минимум 2 поста автора");

        userDbService.deleteUserHard(authorId);
    }
}