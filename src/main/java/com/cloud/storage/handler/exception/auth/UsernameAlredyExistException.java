package com.cloud.storage.handler.exception.auth;

public class UsernameAlredyExistException extends RuntimeException {
    public UsernameAlredyExistException(String message) {
        super(message);
    }
}
