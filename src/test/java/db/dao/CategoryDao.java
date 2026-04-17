package db.dao;

import db.core.DbExecutor;

import java.sql.ResultSet;

/**
 * DAO для работы с рубриками WordPress (таблицы wp_terms, wp_term_taxonomy).
 */
public class CategoryDao {

    /**
     * Проверяет существование рубрики в БД.
     *
     * @param id ID рубрики
     * @return true если рубрика существует, false если нет
     */
    public boolean exists(int id) {
        String sql = "SELECT 1 FROM wp_terms t " +
                "INNER JOIN wp_term_taxonomy tt ON t.term_id = tt.term_id " +
                "WHERE t.term_id = ? AND tt.taxonomy = 'category'";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                ResultSet::next
        );
    }

    /**
     * Возвращает имя рубрики по ID.
     *
     * @param id ID рубрики
     * @return имя рубрики или null если не найдена
     */
    public String getName(int id) {
        String sql = "SELECT name FROM wp_terms WHERE term_id = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает slug рубрики по ID.
     *
     * @param id ID рубрики
     * @return slug рубрики или null если не найдена
     */
    public String getSlug(int id) {
        String sql = "SELECT slug FROM wp_terms WHERE term_id = ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    /**
     * Возвращает общее количество рубрик.
     *
     * @return количество рубрик
     */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM wp_terms t " +
                "INNER JOIN wp_term_taxonomy tt ON t.term_id = tt.term_id " +
                "WHERE tt.taxonomy = 'category'";

        return DbExecutor.query(
                sql,
                null,
                rs -> rs.next() ? rs.getInt(1) : 0
        );
    }

    /**
     * Возвращает количество рубрик с именем LIKE pattern.
     *
     * @param pattern паттерн для LIKE
     * @return количество рубрик
     */
    public int countByNameLike(String pattern) {
        String sql = "SELECT COUNT(*) FROM wp_terms t " +
                "INNER JOIN wp_term_taxonomy tt ON t.term_id = tt.term_id " +
                "WHERE tt.taxonomy = 'category' AND t.name LIKE ?";

        return DbExecutor.query(
                sql,
                ps -> ps.setString(1, pattern),
                rs -> rs.next() ? rs.getInt(1) : 0
        );
    }

    /**
     * Создает новую рубрику в БД.
     *
     * @param name имя рубрики
     * @param slug slug рубрики
     * @return ID созданной рубрики
     */
    public int create(String name, String slug) {
        // Создаем запись в wp_terms
        String sqlTerm = "INSERT INTO wp_terms (name, slug) VALUES (?, ?)";
        int termId = DbExecutor.insert(sqlTerm, ps -> {
            ps.setString(1, name);
            ps.setString(2, slug);
        });

        // Создаем запись в wp_term_taxonomy с пустым description
        String sqlTax = "INSERT INTO wp_term_taxonomy (term_id, taxonomy, description) VALUES (?, 'category', '')";
        DbExecutor.update(sqlTax, ps -> ps.setInt(1, termId));

        return termId;
    }

    /**
     * Удаляет рубрику из БД по ID.
     *
     * @param id ID рубрики
     */
    public void delete(int id) {
        if (id <= 0) return;

        String sqlTaxonomy = "DELETE FROM wp_term_taxonomy WHERE term_id = ? AND taxonomy = 'category'";
        DbExecutor.update(sqlTaxonomy, ps -> ps.setInt(1, id));

        String sqlTerm = "DELETE FROM wp_terms WHERE term_id = ?";
        DbExecutor.update(sqlTerm, ps -> ps.setInt(1, id));
    }
}