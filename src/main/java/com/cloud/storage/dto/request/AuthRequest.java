package com.cloud.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AuthRequest {

    @NotBlank(message = "Username обязательное поле")
    @Size(min = 3, max = 50, message = "Пределы: 3<=username<=50")
    String username;

    @NotBlank(message = "Password обязательное поле")
    @Size(min = 5, max = 32, message = "Пределы: 6<=password<=32")
    String password;
}
