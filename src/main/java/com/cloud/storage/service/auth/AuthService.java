package com.cloud.storage.service.auth;

import com.cloud.storage.dto.request.AuthRequest;
import com.cloud.storage.dto.response.AuthResponse;
import com.cloud.storage.entity.User;
import com.cloud.storage.handler.exception.auth.UnauthorizedException;
import com.cloud.storage.handler.exception.resourse.ResourceAlreadyExistsException;
import com.cloud.storage.mapper.AuthMapper;
import com.cloud.storage.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    AuthMapper authMapper;

    /**
     * <h1>Регистрация нового пользователя
     *
     * @param authRequest - данные для регистрации
     * @return AuthResponse - ответ с информацией о пользователе
     * @throws ResourceAlreadyExistsException - если пользователь уже есть в системе
     */
    @Transactional
    public AuthResponse register(AuthRequest authRequest) {

        log.info("Начинаю регистрацию нового пользователя");
        String username = authRequest.getUsername();

        if (userRepository.existsByUsername(username)) {

            log.warn("Не могу зарегистрировать пользователя который уже есть в системе");
            throw new ResourceAlreadyExistsException("Пользователь c username: " + username + " уже есть в системе");
        }

        log.info("Сохраняю в бд");

        User user = User.builder()
                .username(authRequest.getUsername())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .build();

        userRepository.save(user);
        authenticateUser(username, authRequest.getPassword());
        log.info("Успех");
        return authMapper.toResponse(user);
    }

    /**
     * <h1>Аутентификация пользователя (вход в систему)
     *
     * @param authRequest - данные для входа
     * @return AuthResponse - ответ с информацией о пользователе
     * @throws UnauthorizedException - если логин или пароль неверны
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest authRequest) {

        String username = authRequest.getUsername();
        log.info("Попытка войти в систему под пользователем: {}", username);

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь с username: {} не найден. Вход невозможен", username);
                    return new UnauthorizedException("Неверный логин или пароль");
                });

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {

            log.warn("Неверный пароль для пользователя: {}", username);
            throw new UnauthorizedException("Неверный пароль");
        }

        authenticateUser(username, authRequest.getPassword());
        log.info("Успешная попытка входа");
        return authMapper.toResponse(user);
    }

    /**
     * <h1>Очищает контекст безопасности
     */
    public void logOut() {

        log.info("Очистка SecurityContext");
        SecurityContextHolder.clearContext();
    }

    /**
     * <h1>Аутентифицирует пользователя в Spring Security
     * <h3>Создаёт сессию автоматически через Spring Session
     *
     * @param username - пользователь для создания контекста
     * @param password - пароль для первичной аутентификации
     */
    private void authenticateUser(String username, String password) {
        log.info("аутентификация");
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("успех");
    }
}
