package enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TestDataTemplates {

    POST_TITLE("Test_Post"),
    POST_CONTENT("Lorem ipsum dolor sit amet, consectetur adipiscing elit."),

    CATEGORY_NAME("Test_Category"),

    USER_USERNAME("autotest_user"),

    MEDIA_ALT_TEXT("Test image alt text"),
    MEDIA_TITLE("Test_Image_Title");

    private final String value;

    public String getUniqueValue() {
        return value + "_" + System.currentTimeMillis();
    }

    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.toLowerCase()
                .replace("_", "-")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}