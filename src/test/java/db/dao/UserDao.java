package db.dao;

import db.core.DbExecutor;
import helpers.TestDataGenerator;

import java.sql.ResultSet;
import java.util.Random;

/**
 * DAO для работы с пользователями WordPress (таблица wp_users).
 */
public class UserDao {

    /**
     * Проверяет существование пользователя в БД.
     *
     * @param id ID пользователя
     * @return true если пользователь существует, false если нет
     */
    public boolean userExists(int id) {
        String sql = "SELECT 1 FROM wp_users WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                ResultSet::next
        );
    }

    /**
     * Возвращает email пользователя по ID.
     *
     * @param id ID пользователя
     * @return email пользователя или null если не найден
     */
    public String getEmail(int id) {
        String sql = "SELECT user_email FROM wp_users WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает username пользователя по ID.
     *
     * @param id ID пользователя
     * @return username пользователя или null если не найден
     */
    public String getUsername(int id) {
        String sql = "SELECT user_login FROM wp_users WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает user_nicename пользователя по ID.
     *
     * @param id ID пользователя
     * @return user_nicename пользователя или null если не найден
     */
    public String getNicename(int id) {
        String sql = "SELECT user_nicename FROM wp_users WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает общее количество пользователей.
     *
     * @return количество пользователей
     */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM wp_users";

        return DbExecutor.query(
                sql,
                null,
                rs -> rs.next() ? rs.getInt(1) : 0
        );
    }

    /**
     * Возвращает количество пользователей с login LIKE pattern.
     *
     * @param pattern паттерн для LIKE
     * @return количество пользователей
     */
    public int countByLoginLike(String pattern) {
        String sql = "SELECT COUNT(*) FROM wp_users WHERE user_login LIKE ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setString(1, pattern),
                rs -> rs.next() ? rs.getInt(1) : 0
        );
    }

    /**
     * Создает нового пользователя в БД.
     *
     * @param login    логин
     * @param email    email
     * @param password пароль (будет захеширован MD5)
     * @param nicename nicename (если null, используется login)
     * @return ID созданного пользователя
     */
    public int create(String login, String email, String password, String nicename) {
        String finalNicename = nicename != null ? nicename : login;
        String sql = "INSERT INTO wp_users (user_login, user_email, user_pass, user_nicename, user_registered) " +
                "VALUES (?, ?, MD5(?), ?, NOW())";
        return DbExecutor.insert(sql, ps -> {
            ps.setString(1, login);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, finalNicename);
        });
    }

    /**
     * Создает пользователя с указанным nicename.
     *
     * @param login    логин
     * @param email    email
     * @param nicename nicename
     * @return ID созданного пользователя
     */
    public int createWithNicename(String login, String email, String password, String nicename) {
        return create(login, email, password, nicename);
    }

    /**
     * Удаляет пользователя из БД по ID.
     *
     * @param id ID пользователя
     */
    public void delete(int id) {
        if (id <= 0) return;

        // Сначала удаляем метаданные (внешний ключ)
        String sqlMeta = "DELETE FROM wp_usermeta WHERE user_id = ?";
        DbExecutor.update(sqlMeta, ps -> ps.setInt(1, id));

        // Затем удаляем пользователя
        String sqlUser = "DELETE FROM wp_users WHERE ID = ?";
        DbExecutor.update(sqlUser, ps -> ps.setInt(1, id));
    }

    /**
     * Возвращает ID автора поста.
     *
     * @param postId ID поста
     * @return ID автора или -1 если пост не найден
     */
    public int getPostAuthorId(int postId) {
        String sql = "SELECT post_author FROM wp_posts WHERE ID = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, postId),
                rs -> rs.next() ? rs.getInt(1) : -1
        );
    }
}