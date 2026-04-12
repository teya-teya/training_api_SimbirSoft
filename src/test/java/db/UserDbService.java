package db;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_users, wp_posts, wp_usermeta).
 */
@Slf4j
public class UserDbService {

    private Connection getConnection() throws SQLException {
        return DbConnection.getDataSource().getConnection();
    }

    @Step("Проверка существования пользователя в БД по ID: {id}")
    public boolean isUserExists(int id) throws SQLException {
        String sql = "SELECT 1 FROM wp_users WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                if (!exists) {
                    log.warn("Пользователь ID={} не найден в БД", id);
                }
                return exists;
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования пользователя ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение email пользователя из БД по ID: {id}")
    public String getUserEmail(int id) throws SQLException {
        String sql = "SELECT user_email FROM wp_users WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("user_email") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения email пользователя ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение username пользователя из БД по ID: {id}")
    public String getUserUsername(int id) throws SQLException {
        String sql = "SELECT user_login FROM wp_users WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("user_login") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения username пользователя ID={}", id, e);
            throw e;
        }
    }

    @Step("Проверка, что посты пользователя {oldAuthorId} переназначены пользователю {newAuthorId}")
    public boolean arePostsReassignedTo(int oldAuthorId, int newAuthorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_author = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, oldAuthorId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = rs.next() ? rs.getInt(1) : 0;
                log.info("Постов с автором ID={} осталось: {}", oldAuthorId, count);
                return count == 0;
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки переназначения постов от {} к {}", oldAuthorId, newAuthorId, e);
            throw e;
        }
    }

    @Step("Получение количества постов у автора {authorId}")
    public int getPostsCountByAuthor(int authorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_author = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Step("Получение автора поста {postId}")
    public int getPostAuthorId(int postId) throws SQLException {
        String sql = "SELECT post_author FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("post_author") : -1;
            }
        }
    }

    @Step("Получение количества всех пользователей в БД")
    public int getUsersCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wp_users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int count = rs.next() ? rs.getInt(1) : 0;
            log.info("Всего пользователей в БД: {}", count);
            return count;
        } catch (SQLException e) {
            log.error("Ошибка подсчёта пользователей", e);
            throw e;
        }
    }

    @Step("Принудительное удаление пользователя из БД по ID: {id}")
    public void deleteUserHard(int id) throws SQLException {
        String sqlMeta = "DELETE FROM wp_usermeta WHERE user_id = ?";
        String sqlUser = "DELETE FROM wp_users WHERE ID = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtMeta = conn.prepareStatement(sqlMeta);
                 PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {

                stmtMeta.setInt(1, id);
                stmtMeta.executeUpdate();

                stmtUser.setInt(1, id);
                stmtUser.executeUpdate();

                conn.commit();
                log.info("Удалён пользователь ID={} из БД", id);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Ошибка удаления пользователя ID={}", id, e);
            throw e;
        }
    }
}