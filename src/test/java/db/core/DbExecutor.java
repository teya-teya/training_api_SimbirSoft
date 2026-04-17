package db.core;

import db.DbConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class DbExecutor {

    /**
     * Выполняет SELECT запрос и возвращает результат.
     *
     * @param sql       SQL запрос
     * @param setter    установщик параметров (может быть null)
     * @param extractor преобразователь ResultSet в нужный тип
     * @param <T>       тип возвращаемого значения
     * @return результат выполнения запроса
     * @throws RuntimeException если произошла ошибка БД
     */
    public static <T> T query(String sql,
                              PreparedStatementSetter setter,
                              ResultSetExtractor<T> extractor) {
        try (Connection conn = DbConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (setter != null) {
                setter.setValues(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return extractor.extract(rs);
            }

        } catch (SQLException e) {
            log.error("DB query error: {}", sql, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполняет INSERT запрос и возвращает сгенерированный ID.
     *
     * @param sql    SQL запрос
     * @param setter установщик параметров (может быть null)
     * @return сгенерированный ID
     * @throws RuntimeException если произошла ошибка БД
     */
    public static int insert(String sql,
                             PreparedStatementSetter setter) {
        try (Connection conn = DbConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (setter != null) {
                setter.setValues(stmt);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            log.error("DB insert error: {}", sql, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполняет UPDATE, DELETE или INSERT (без возврата ID) операцию в БД.
     *
     * @param sql    SQL запрос
     * @param setter установщик параметров (может быть null)
     * @throws RuntimeException если произошла ошибка БД
     */
    public static void update(String sql, PreparedStatementSetter setter) {
        try (Connection conn = DbConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.setValues(stmt);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("DB update error: {}", sql, e);
            throw new RuntimeException(e);
        }
    }
}