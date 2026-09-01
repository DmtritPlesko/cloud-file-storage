package com.cloud.storage.controller.imp;

import com.cloud.storage.annotation.CurrentUser;
import com.cloud.storage.dto.response.AuthResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/user")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserControllerImpl {

    @GetMapping(path = "/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal
                                @CurrentUser String username) {

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Запрос данных пользователя: {}", username);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new AuthResponse(username));
    }
}

