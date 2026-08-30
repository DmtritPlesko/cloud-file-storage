package com.cloud.storage.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Аннотация для проверки сущестовования UserDetails и кастомный возврат
 * проверяет:
 * 1. Пустой ли UserDetails
 * <p>
 * плюшки:
 * 1. Сразу возвращает нам username/UUID пользователя в зависимости от параметра метода
 * 2. Как следствие -> Снижение нагрузки на БД когда кладём файлик в хранилище (из за того что работаем в контексте спринга)
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {

    String message() default "Пользователь не авторизован";
}
