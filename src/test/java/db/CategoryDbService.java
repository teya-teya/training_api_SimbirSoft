package db;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * Сервис для работы с базой данных WordPress (таблицы wp_terms, wp_term_taxonomy).
 */
@Slf4j
public class CategoryDbService {

    private Connection getConnection() throws SQLException {
        return DbConnection.getDataSource().getConnection();
    }

    @Step("Проверка существования рубрики в БД по ID: {id}")
    public boolean isCategoryExists(int id) throws SQLException {
        String sql = "SELECT 1 FROM wp_terms t " +
                "INNER JOIN wp_term_taxonomy tt ON t.term_id = tt.term_id " +
                "WHERE t.term_id = ? AND tt.taxonomy = 'category'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                if (!exists) {
                    log.warn("Рубрика ID={} не найдена в БД", id);
                }
                return exists;
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования рубрики ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение имени рубрики из БД по ID: {id}")
    public String getCategoryName(int id) throws SQLException {
        String sql = "SELECT name FROM wp_terms WHERE term_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        } catch (SQLException e) {
            log.error("Ошибка получения имени рубрики ID={}", id, e);
            throw e;
        }
    }

    @Step("Получение количества всех рубрик в БД")
    public int getCategoriesCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wp_terms t " +
                "INNER JOIN wp_term_taxonomy tt ON t.term_id = tt.term_id " +
                "WHERE tt.taxonomy = 'category'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int count = rs.next() ? rs.getInt(1) : 0;
            log.info("Всего рубрик в БД: {}", count);
            return count;
        } catch (SQLException e) {
            log.error("Ошибка подсчёта рубрик", e);
            throw e;
        }
    }

    @Step("Принудительное удаление рубрики из БД по ID: {id}")
    public void deleteCategoryHard(int id) throws SQLException {
        // Сначала удаляем из wp_term_taxonomy (из-за внешнего ключа)
        String sqlTaxonomy = "DELETE FROM wp_term_taxonomy WHERE term_id = ? AND taxonomy = 'category'";
        String sqlTerm = "DELETE FROM wp_terms WHERE term_id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtTaxonomy = conn.prepareStatement(sqlTaxonomy);
                 PreparedStatement stmtTerm = conn.prepareStatement(sqlTerm)) {

                stmtTaxonomy.setInt(1, id);
                stmtTaxonomy.executeUpdate();

                stmtTerm.setInt(1, id);
                stmtTerm.executeUpdate();

                conn.commit();
                log.info("Удалена рубрика ID={} из БД", id);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Ошибка удаления рубрики ID={}", id, e);
            throw e;
        }
    }
}