package com.cloud.storage.controller.imp;

import com.cloud.storage.controller.AuthControllerApi;
import com.cloud.storage.dto.request.AuthRequest;
import com.cloud.storage.dto.response.AuthResponse;
import com.cloud.storage.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthControllerImpl implements AuthControllerApi {

    AuthService authService;

    @PostMapping("/sign-in")
    @Override
    public ResponseEntity<AuthResponse> login(@RequestBody
                                   @Valid AuthRequest request) {

        log.info("Вход под пользователем: {}", request.getUsername());


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authService.authenticate(request));
    }

    @PostMapping("/sign-up")
    @Override
    public ResponseEntity<AuthResponse> register(@RequestBody
                                      @Valid AuthRequest request) {

        log.info("Регистрация: {} пользователя", request.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/sign-out")
    @Override
    public ResponseEntity<Void> logout() {

        log.info("Выполняю выход");
        authService.logOut();

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
