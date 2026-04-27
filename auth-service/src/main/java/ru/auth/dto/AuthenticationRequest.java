package ru.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Запрос на аутентификацию")
public class AuthenticationRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 64, message = "username length must be 3 - 64")
    @Schema(description = "Имя пользователя", example = "user123", required = true, minLength = 3, maxLength = 64)
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 128, message = "password length must be 6 - 128")
    @Schema(description = "Пароль", example = "password123", required = true, minLength = 6, maxLength = 128)
    private String password;

}





