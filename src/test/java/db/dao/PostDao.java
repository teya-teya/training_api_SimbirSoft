package db.dao;

import db.core.DbExecutor;

import java.sql.ResultSet;

/**
 * DAO для работы с записями WordPress (таблица wp_posts, тип 'post').
 */
public class PostDao {

    /**
     * Проверяет существование записи в БД по ID.
     *
     * @param id ID записи
     * @return true если запись существует, false если нет
     */
    public boolean postExists(int id) {
        String sql = "SELECT 1 FROM wp_posts WHERE ID = ? AND post_type = 'post'";
        return DbExecutor.query(sql, ps -> ps.setInt(1, id), ResultSet::next);
    }

    /**
     * Возвращает значение указанного поля для записи.
     *
     * @param id    ID записи
     * @param field имя поля (post_title, post_content, post_status, post_type)
     * @return значение поля или null если запись не найдена
     */
    public String getField(int id, String field) {
        String sql = "SELECT " + field + " FROM wp_posts WHERE ID = ?";
        return DbExecutor.query(sql, ps -> ps.setInt(1, id), rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * Создает новую запись в БД.
     *
     * @param title   заголовок
     * @param content содержимое
     * @param status  статус (draft, publish и т.д.)
     * @return ID созданной записи
     */
    public int create(String title, String content, String status) {
        String sql = "INSERT INTO wp_posts (post_title, post_content, post_status, post_type, " +
                "post_excerpt, to_ping, pinged, post_content_filtered, " +
                "post_date, post_date_gmt, post_modified, post_modified_gmt) " +
                "VALUES (?, ?, ?, 'post', '', '', '', '', NOW(), NOW(), NOW(), NOW())";
        return DbExecutor.insert(sql, ps -> {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, status);
        });
    }

    /**
     * Возвращает количество записей с заголовком, содержащим указанный паттерн.
     *
     * @param pattern паттерн для LIKE (например, "autotest_%")
     * @return количество записей
     */
    public int countByTitleLike(String pattern) {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_title LIKE ?";
        return DbExecutor.query(sql, ps -> ps.setString(1, pattern), rs -> rs.next() ? rs.getInt(1) : 0);
    }

    /**
     * Возвращает количество записей с указанным статусом и паттерном заголовка.
     *
     * @param status  статус записи (draft, publish)
     * @param pattern паттерн для LIKE
     * @return количество записей
     */
    public int countByStatusAndTitleLike(String status, String pattern) {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_status = ? AND post_title LIKE ?";
        return DbExecutor.query(sql, ps -> {
            ps.setString(1, status);
            ps.setString(2, pattern);
        }, rs -> rs.next() ? rs.getInt(1) : 0);
    }

    /**
     * Возвращает количество записей указанного автора с паттерном заголовка.
     *
     * @param authorId ID автора
     * @param pattern  паттерн для LIKE
     * @return количество записей
     */
    public int countByAuthorAndTitleLike(int authorId, String pattern) {
        String sql = "SELECT COUNT(*) FROM wp_posts WHERE post_author = ? AND post_title LIKE ?";
        return DbExecutor.query(sql, ps -> {
            ps.setInt(1, authorId);
            ps.setString(2, pattern);
        }, rs -> rs.next() ? rs.getInt(1) : 0);
    }

    /**
     * Создает запись для указанного автора.
     *
     * @param title    заголовок
     * @param content  содержимое
     * @param status   статус
     * @param authorId ID автора
     * @return ID созданной записи
     */
    public int createForAuthor(String title, String content, String status, int authorId) {
        String sql = "INSERT INTO wp_posts (post_title, post_content, post_status, post_type, post_author, " +
                "post_excerpt, to_ping, pinged, post_content_filtered, " +
                "post_date, post_date_gmt, post_modified, post_modified_gmt) " +
                "VALUES (?, ?, ?, 'post', ?, '', '', '', '', NOW(), NOW(), NOW(), NOW())";
        return DbExecutor.insert(sql, ps -> {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, status);
            ps.setInt(4, authorId);
        });
    }

    /**
     * Удаляет запись из БД по ID.
     *
     * @param id ID записи (если id <= 0, метод ничего не делает)
     */
    public void delete(int id) {
        if (id <= 0) return;
        DbExecutor.update("DELETE FROM wp_posts WHERE ID = ?", ps -> ps.setInt(1, id));
    }
}