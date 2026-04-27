package ru.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.auth.entity.Role;

@Data
@Schema(description = "Запрос на регистрацию пользователя")
public class SignupRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 64, message = "username length must be 3 - 64")
    @Schema(description = "Имя пользователя (email)", example = "teacher@demo.com", required = true)
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 128, message = "password length must be 6 - 128")
    @Schema(description = "Пароль", example = "password123", required = true)
    private String password;

    @NotNull(message = "role must not be null")
    @Schema(description = "Роль пользователя", example = "ROLE_TEACHER", required = true)
    private Role role;
}


