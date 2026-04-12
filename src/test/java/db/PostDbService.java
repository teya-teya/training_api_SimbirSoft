package db;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * Сервис для работы с базой данных WordPress (таблица wp_posts).
 */
@Slf4j
public class PostDbService {

    private Connection getConnection() throws SQLException {
        return DbConnection.getDataSource().getConnection();
    }

    @Step("Проверка существования записи в БД по ID: {id}")
    public boolean isPostExists(int id) throws SQLException {
        String sql = "SELECT 1 FROM wp_posts WHERE ID = ? AND post_type = 'post'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                if (!exists) {
                    log.warn("Запись ID={} не найдена в БД", id);
                }
                return exists;
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования записи ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение заголовка записи из БД по ID: {id}")
    public String getPostTitle(int id) throws SQLException {
        String sql = "SELECT post_title FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_title") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения заголовка записи ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение содержимого записи из БД по ID: {id}")
    public String getPostContent(int id) throws SQLException {
        String sql = "SELECT post_content FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_content") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения содержимого записи ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение статуса записи из БД по ID: {id}")
    public String getPostStatus(int id) throws SQLException {
        String sql = "SELECT post_status FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_status") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения статуса записи ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение типа записи из БД по ID: {id}")
    public String getPostType(int id) throws SQLException {
        String sql = "SELECT post_type FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_type") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения типа записи ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение количества всех записей в БД")
    public int getPostsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_type = 'post'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int count = rs.next() ? rs.getInt(1) : 0;
            log.info("Всего записей в БД: {}", count);
            return count;
        } catch (SQLException e) {
            log.error("Ошибка подсчёта записей", e);
            throw e;
        }
    }

    @Step("Принудительное удаление записи из БД по ID: {id}")
    public void deletePostHard(int id) throws SQLException {
        String sql = "DELETE FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Удалена запись ID={} из БД", id);
        } catch (SQLException e) {
            log.error("Ошибка удаления записи ID={}", id, e);
            throw e;
        }
    }

    public void deleteAllTestPosts() throws SQLException {
        String sql = "DELETE FROM wp_posts WHERE post_type = 'post' AND ID != 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            log.info("Удалены все тестовые посты");
        }
    }
}