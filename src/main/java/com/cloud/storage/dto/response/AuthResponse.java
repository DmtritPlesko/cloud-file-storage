package com.cloud.storage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ о аутнтификации/регистрации")
public class AuthResponse {

    @Schema(description = "юз пользака")
    String username;
}
