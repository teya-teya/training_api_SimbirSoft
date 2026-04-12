package db;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_posts, wp_postmeta).
 */
@Slf4j
public class MediaDbService {

    private Connection getConnection() throws SQLException {
        return DbConnection.getDataSource().getConnection();
    }

    @Step("Проверка существования медиафайла в БД по ID: {id}")
    public boolean isMediaExists(int id) throws SQLException {
        String sql = "SELECT 1 FROM wp_posts WHERE ID = ? AND post_type = 'attachment'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                if (!exists) {
                    log.warn("Медиафайл ID={} не найден в БД", id);
                }
                return exists;
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования медиафайла ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение mime_type медиафайла из БД по ID: {id}")
    public String getMimeType(int id) throws SQLException {
        String sql = "SELECT post_mime_type FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_mime_type") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения mime_type медиафайла ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение заголовка медиафайла из БД по ID: {id}")
    public String getMediaTitle(int id) throws SQLException {
        String sql = "SELECT post_title FROM wp_posts WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("post_title") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения заголовка медиафайла ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение alt_text медиафайла из БД по ID: {id}")
    public String getAltText(int id) throws SQLException {
        String sql = "SELECT meta_value FROM wp_postmeta WHERE post_id = ? AND meta_key = '_wp_attachment_image_alt'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("meta_value") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения alt_text медиафайла ID={}", id, e);
            throw e;
        }
    }

    @Step("Проверка наличия метаданных _wp_attachment_metadata у медиафайла ID: {id}")
    public boolean hasMetadata(int id) throws SQLException {
        String sql = "SELECT 1 FROM wp_postmeta WHERE post_id = ? AND meta_key = '_wp_attachment_metadata'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки метаданных медиафайла ID={}", id, e);
            throw e;
        }
    }

    @Step("Принудительное удаление медиафайла из БД по ID: {id}")
    public void deleteMediaHard(int id) throws SQLException {
        String sqlMeta = "DELETE FROM wp_postmeta WHERE post_id = ?";
        String sqlPost = "DELETE FROM wp_posts WHERE ID = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtMeta = conn.prepareStatement(sqlMeta);
                 PreparedStatement stmtPost = conn.prepareStatement(sqlPost)) {

                stmtMeta.setInt(1, id);
                stmtMeta.executeUpdate();

                stmtPost.setInt(1, id);
                stmtPost.executeUpdate();

                conn.commit();
                log.info("Удалён медиафайл ID={} из БД", id);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Ошибка удаления медиафайла ID={}", id, e);
            throw e;
        }
    }
}