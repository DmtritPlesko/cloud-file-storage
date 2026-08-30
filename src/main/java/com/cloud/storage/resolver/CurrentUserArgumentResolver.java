package com.cloud.storage.resolver;

import com.cloud.storage.annotation.CurrentUser;
import com.cloud.storage.handler.exception.auth.UnauthorizedException;
import com.cloud.storage.service.auth.custom.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;


/**
 * Резолвер который возвращает правильный тип парметра в зависимости от передаваемух (в ResourceController/UserCOntroller)
 * <p>Основная идея интерпритация DRY для перманентной провервки UserDetails и извлечение username/UUID
 * + снижение нагрузки на бд
 * <p>
 * <br>
 * (Работает в связке с аннотацией @CurrentUser)
 */
@Slf4j
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                (parameter.getParameterType().equals(String.class) ||
                        parameter.getParameterType().equals(UUID.class));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() == null) {

            CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
            String message = annotation != null ? annotation.message() : "Пользователь не авторизован";

            log.warn("Попытка доступа без авторизации к методу: {}",
                    parameter.getMethod().getName());
            throw new UnauthorizedException(message);
        }

        Object principal = authentication.getPrincipal();

        Class<?> paramType = parameter.getParameterType();

        if (paramType.equals(String.class)) {

            String username = principal instanceof UserDetails ?
                    ((UserDetails) principal).getUsername() : principal.toString();
            log.info("Возврат username: {}", username);
            return username;

        } else if (paramType.equals(UUID.class)) {

            if (principal instanceof CustomUserDetails) {

                UUID userId = ((CustomUserDetails) principal).getUserId();
                log.info("Возврат userId: {}", userId);
                return userId;
            }
        }

        throw new UnauthorizedException("Невалидный пользователь");
    }
}
