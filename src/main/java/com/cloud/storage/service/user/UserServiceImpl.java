package com.cloud.storage.service.user;

import com.cloud.storage.entity.User;
import com.cloud.storage.handler.exception.UserNotFoundException;
import com.cloud.storage.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    @Override
    public User findByUsername(String username) {

        log.info("Поиск пользователя {}", username);

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не существует", username);
                    return new UserNotFoundException("Пользоватлеь не найден");
                });
    }
}
