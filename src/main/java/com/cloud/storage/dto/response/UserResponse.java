package com.cloud.storage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответка о успешном создаинни/регистарции пользака")
public class UserResponse {

    @Schema(description = "Имя пользака")
    String username;
}
