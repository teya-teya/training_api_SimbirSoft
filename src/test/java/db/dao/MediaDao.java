package db.dao;

import db.core.DbExecutor;

import java.sql.ResultSet;

/**
 * DAO для работы с медиафайлами WordPress (таблица wp_posts, тип 'attachment').
 */
public class MediaDao {

    /**
     * Проверяет существование медиафайла в БД.
     *
     * @param id ID медиафайла
     * @return true если медиафайл существует, false если нет
     */
    public boolean fileExists(int id) {
        String sql = "SELECT 1 FROM wp_posts WHERE ID = ? AND post_type = 'attachment'";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                ResultSet::next
        );
    }

    /**
     * Возвращает MIME тип медиафайла по ID.
     *
     * @param id ID медиафайла
     * @return MIME тип или null если не найден
     */
    public String getMimeType(int id) {
        String sql = "SELECT post_mime_type FROM wp_posts WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает заголовок медиафайла по ID.
     *
     * @param id ID медиафайла
     * @return заголовок или null если не найден
     */
    public String getTitle(int id) {
        String sql = "SELECT post_title FROM wp_posts WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает post_type медиафайла по ID.
     *
     * @param id ID медиафайла
     * @return post_type или null если не найден
     */
    public String getPostType(int id) {
        String sql = "SELECT post_type FROM wp_posts WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает alt_text медиафайла из метаданных.
     *
     * @param id ID медиафайла
     * @return alt_text или null если не найден
     */
    public String getAltText(int id) {
        String sql = "SELECT meta_value FROM wp_postmeta WHERE post_id = ? AND meta_key = '_wp_attachment_image_alt'";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Проверяет наличие метаданных _wp_attachment_metadata у медиафайла.
     *
     * @param id ID медиафайла
     * @return true если метаданные есть, false если нет
     */
    public boolean hasMetadata(int id) {
        String sql = "SELECT 1 FROM wp_postmeta WHERE post_id = ? AND meta_key = '_wp_attachment_metadata'";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                ResultSet::next
        );
    }

    /**
     * Создает тестовый attachment в БД.
     *
     * @param mimeType    MIME тип (например, "image/png")
     * @param guidPattern паттерн для guid
     * @return ID созданного медиафайла
     */
    public int createTestAttachment(String mimeType, String guidPattern) {
        String sql = "INSERT INTO wp_posts (post_type, post_mime_type, guid, post_status, " +
                "post_title, post_content, post_excerpt, to_ping, pinged, post_content_filtered, " +
                "post_date, post_date_gmt, post_modified, post_modified_gmt) " +
                "VALUES ('attachment', ?, CONCAT('http://test.com/', ?, '.png'), 'inherit', " +
                "'', '', '', '', '', '', NOW(), NOW(), NOW(), NOW())";

        return DbExecutor.insert(sql, ps -> {
            ps.setString(1, mimeType);
            ps.setString(2, guidPattern);
        });
    }

    /**
     * Удаляет медиафайл из БД по ID.
     *
     * @param id ID медиафайла
     */
    public void delete(int id) {
        if (id <= 0) return;

        String sqlMeta = "DELETE FROM wp_postmeta WHERE post_id = ?";
        DbExecutor.update(sqlMeta, ps -> ps.setInt(1, id));

        String sqlPost = "DELETE FROM wp_posts WHERE ID = ?";
        DbExecutor.update(sqlPost, ps -> ps.setInt(1, id));
    }
}