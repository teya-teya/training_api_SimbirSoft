package tests.d2;

import api.BaseApi.AuthType;
import checks.Checks;
import client.PostsClient;
import db.service.PostDbService;
import db.service.UserDbService;
import enums.TestDataTemplates;
import helpers.TestDataGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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

    private final List<Integer> postIds = new ArrayList<>();
    private final String statusPublic = "publish";
    private String postTitle, postContent, username, email, password;

    @BeforeMethod
    public void create() {
        postTitle = TestDataTemplates.POST_TITLE.getUniqueValue();
        postContent = TestDataTemplates.POST_CONTENT.getUniqueValue();
        username = TestDataTemplates.USER_USERNAME.getUniqueValue();
        email = TestDataGenerator.getRandomEmail();
        password = TestDataGenerator.getRandomPassword();
    }

    @AfterMethod
    public void cleanUp() {
        postDbService.deletePostsHard(postIds);
        postIds.clear();


    }

    @Test(description = "TC-POST-01-D2: Получение списка записей")
    public void tcPost01D2_getPostsList() {

        postIds.addAll(postDbService.createTestPostsWithStatus(3, statusPublic));

        Response response = postsClient.getPosts(AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        List<Integer> ids = response.jsonPath().getList("id");

        Checks.checkListContainsAll(postIds, ids, "Ответ должен содержать пост ");

        int dbCount = postDbService.getPostsCountByTitleLike(TestDataTemplates.POST_TITLE.getValue() + "%");
        Checks.checkTrue(dbCount >= 3, "В БД должно быть минимум 3 тестовых поста");
    }

    @Test(description = "TC-POST-02-D2: Получение записи по ID")
    public void tcPost02D2_getPostById() {

        int postId = postDbService.createTestPost(postTitle, postContent, statusPublic);
        postIds.add(postId);

        Response response = postsClient.getPostById(postId, AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        Checks.checkEquals(response.jsonPath().getString("title.rendered"), postTitle, "title должен совпадать");
        Checks.checkEquals(response.jsonPath().getString("content.rendered").contains(postContent), true, "content должен содержать текст");
        Checks.checkEquals(response.jsonPath().getString("status"), statusPublic, "status должен быть " + statusPublic);

        Checks.checkEquals(postDbService.getPostTitle(postId), postTitle, "Заголовок в БД");
        Checks.checkEquals(postDbService.getPostContent(postId), postContent, "Контент в БД");
        Checks.checkEquals(postDbService.getPostStatus(postId), statusPublic, "Статус в БД");
    }

    @Test(description = "TC-POST-03-D2: Фильтр по статусу (draft)")
    public void tcPost03D2_filterByStatusDraft() {

        String statusDraft = "draft";
        postIds.addAll(postDbService.createTestPostsWithStatus(2, statusDraft));

        postIds.add(postDbService.createTestPost(postTitle + statusPublic, postContent, statusPublic));

        Response response = postsClient.getPostsByStatus(statusDraft, AuthType.ADMIN);
        Checks.checkStatusCode(response, 200);

        List<String> statuses = response.jsonPath().getList("status");

        Checks.checkAllMatch(statuses, statusDraft, "Все посты должны быть draft");

        int dbCount = postDbService.getPostsCountByStatusAndTitleLike(statusDraft, TestDataTemplates.POST_TITLE.getValue() + "%");
        Checks.checkTrue(dbCount >= 2, "В БД должно быть минимум 2 draft поста");
    }

    @Test(description = "TC-POST-04-D2: Фильтр по автору")
    public void tcPost04D2_filterByAuthor() {
        int authorId = 0;
        try {
            authorId = userDbService.createTestUser(username, email, password);

            postIds.addAll(postDbService.createTestPostsForAuthor(2, statusPublic, authorId));

            Response response = postsClient.getPostsByAuthor(authorId, AuthType.ADMIN);
            Checks.checkStatusCode(response, 200);

            List<Integer> authors = response.jsonPath().getList("author");
            Checks.checkAllMatch(authors, authorId, "Все посты принадлежат автору");

            int dbCount = postDbService.getPostsCountByAuthorAndTitleLike(authorId, TestDataTemplates.POST_TITLE.getValue() + "%");
            Checks.checkTrue(dbCount >= 2, "В БД должно быть минимум 2 поста автора");
        } finally {
            userDbService.deleteUserHard(authorId);
        }
    }
}