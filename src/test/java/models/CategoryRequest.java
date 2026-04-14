package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания/обновления рубрики (category).
 * <p>
 * Используется в запросах к /wp/v2/categories
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    private String name;
    private String slug;
    private Integer parent;
}