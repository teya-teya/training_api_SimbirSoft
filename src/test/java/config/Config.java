package config;

/**
 * Класс для чтения конфигурации из переменных окружения.
 */
public class Config {

    private static String getEnv(String key) {
        String value = System.getenv(key);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Не задана переменная окружения: " + key);
        }
        return value;
    }

    public static String getBaseUrl() {
        return getEnv("BASE_URL");
    }

    public static String getApiUser() {
        return getEnv("API_USER");
    }

    public static String getApiPassword() {
        return getEnv("API_PASS");
    }

    public static String getDbUrl() {
        return getEnv("DB_URL");
    }

    public static String getDbUser() {
        return getEnv("DB_USER");
    }

    public static String getDbPassword() {
        return getEnv("DB_PASS");
    }
}