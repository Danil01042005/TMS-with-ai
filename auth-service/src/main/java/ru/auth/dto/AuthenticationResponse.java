package ru.auth.dto;

import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;

@Value
@Schema(description = "Ответ с access token")
public class AuthenticationResponse {
    @Schema(description = "JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    String jwt;
}





