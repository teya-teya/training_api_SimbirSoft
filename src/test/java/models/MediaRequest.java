package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления медиафайла (media).
 * <p>
 * Используется в запросах к /wp/v2/media/{id}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaRequest {
    private String title;
    @JsonProperty("alt_text")
    private String altText;
}