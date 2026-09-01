package com.cloud.storage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@Schema(description = "Запрос на аутентификацию/регистрацию")
public class AuthRequest {

    @NotBlank(message = "Username обязательное поле")
    @Size(min = 3, max = 50, message = "Пределы: 3<=username<=50")
    @Schema(description = "юз пользака")
    String username;

    @NotBlank(message = "Password обязательное поле")
    @Size(min = 5, max = 32, message = "Пределы: 6<=password<=32")
    @Schema(description = "пасс пользака")
    String password;
}
