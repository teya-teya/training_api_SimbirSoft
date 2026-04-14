package helpers;

import java.io.File;

public class FileHelper {

    public static File getTestFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("Файл не найден: " + path);
        }
        return file;
    }
}
