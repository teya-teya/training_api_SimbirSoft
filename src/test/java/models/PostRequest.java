package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания/обновления записей (posts).
 * <p>
 * Используется в запросах к /wp/v2/posts
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    private String title;
    private String content;
    private String status;
}