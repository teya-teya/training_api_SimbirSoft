package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания/обновления пользователя (user).
 * <p>
 * Используется в запросах к /wp/v2/users
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
