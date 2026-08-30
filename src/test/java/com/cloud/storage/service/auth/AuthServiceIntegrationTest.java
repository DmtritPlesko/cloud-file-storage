package com.cloud.storage.service.auth;

import com.cloud.storage.dto.request.AuthRequest;
import com.cloud.storage.dto.response.AuthResponse;
import com.cloud.storage.entity.User;
import com.cloud.storage.handler.exception.auth.UnauthorizedException;
import com.cloud.storage.handler.exception.resourse.ResourceAlreadyExistsException;
import com.cloud.storage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Интеграционные тесты для AuthService")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private AuthRequest validAuthRequest;
    private final String TEST_USERNAME = "testuser";
    private final String TEST_PASSWORD = "Test@1234";

    @BeforeEach
    void setUp() {

        validAuthRequest = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();
    }

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    void register_ShouldRegisterNewUser_WhenValidDataProvided() {

        long countBefore = userRepository.count();

        AuthResponse response = authService.register(validAuthRequest);

        long countAfter = userRepository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);

        User savedUser = userRepository.findByUsernameIgnoreCase(TEST_USERNAME)
                .orElseThrow(() -> new AssertionError("Пользователь не найден в БД"));
        assertThat(savedUser.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Ошибка при регистрации существующего пользователя")
    void register_ShouldThrowException_WhenUserAlreadyExists() {

        authService.register(validAuthRequest);

        assertThatThrownBy(() -> authService.register(validAuthRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Пользователь c username: " + TEST_USERNAME + " уже есть в системе");

        long count = userRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Ошибка при регистрации с уже существующим username (регистронезависимо)")
    void register_ShouldThrowException_WhenUserExistsCaseInsensitive() {

        AuthRequest request1 = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();
        authService.register(request1);

        AuthRequest request2 = AuthRequest.builder()
                .username(TEST_USERNAME.toUpperCase())
                .password("AnotherPass123!")
                .build();


        assertThatThrownBy(() -> authService.register(request2))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Пароль шифруется перед сохранением в БД")
    void register_ShouldEncodePassword_WhenRegisterNewUser() {

        authService.register(validAuthRequest);


        User savedUser = userRepository.findByUsernameIgnoreCase(TEST_USERNAME).orElseThrow();
        assertThat(savedUser.getPassword())
                .isNotEqualTo(TEST_PASSWORD)
                .startsWith("$2a$")
                .hasSize(60);
    }

    @Test
    @DisplayName("Успешная аутентификация (вход)")
    void authenticate_ShouldLoginSuccessfully_WhenValidCredentials() {

        authService.register(validAuthRequest);


        AuthResponse response = authService.authenticate(validAuthRequest);


        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("Ошибка при входе с неверным логином")
    void authenticate_ShouldThrowException_WhenUserNotFound() {

        assertThatThrownBy(() -> authService.authenticate(validAuthRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Неверный логин или пароль");
    }

    @Test
    @DisplayName("Ошибка при входе с неверным паролем")
    void authenticate_ShouldThrowException_WhenPasswordIsInvalid() {

        authService.register(validAuthRequest);

        AuthRequest invalidRequest = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password("WrongPassword123!")
                .build();

        assertThatThrownBy(() -> authService.authenticate(invalidRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Неверный пароль");
    }

    @Test
    @DisplayName("Выход из системы — очистка контекста безопасности")
    void logOut_ShouldClearSecurityContext() {

        authService.register(validAuthRequest);
        authService.authenticate(validAuthRequest);

        authService.logOut();

        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    @DisplayName("Регистрация двух разных пользователей — оба успешно сохраняются")
    void register_ShouldSaveMultipleUsers_WhenDifferentUsernames() {

        AuthRequest user1 = AuthRequest.builder()
                .username("user1")
                .password("Pass123!")
                .build();

        AuthRequest user2 = AuthRequest.builder()
                .username("user2")
                .password("Pass456!")
                .build();

        authService.register(user1);
        authService.register(user2);

        long count = userRepository.count();
        assertThat(count).isEqualTo(2);

        assertThat(userRepository.findByUsernameIgnoreCase("user1")).isPresent();
        assertThat(userRepository.findByUsernameIgnoreCase("user2")).isPresent();
    }

    @Test
    @DisplayName("При регистрации автоматически создаётся сессия")
    void register_ShouldCreateSession_WhenNewUserRegistered() {

        authService.register(validAuthRequest);

        Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(TEST_USERNAME);
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Граничный тест: длинное имя пользователя (максимальная длина)")
    void register_ShouldHandleLongUsername_WhenUsernameIsValid() {

        String longUsername = "a".repeat(50);
        AuthRequest request = AuthRequest.builder()
                .username(longUsername)
                .password("Pass123!")
                .build();

        AuthResponse response = authService.register(request);

        assertThat(response.getUsername()).isEqualTo(longUsername);
    }
}
