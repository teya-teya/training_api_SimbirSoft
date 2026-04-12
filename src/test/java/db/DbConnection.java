package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.Config;

import javax.sql.DataSource;

public class DbConnection {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.getDbUrl());
        config.setUsername(Config.getDbUser());
        config.setPassword(Config.getDbPassword());
        config.setMaximumPoolSize(5);

        // Закрывать соединения при завершении
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);

        // Shutdown hook для закрытия пула
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        }));
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}