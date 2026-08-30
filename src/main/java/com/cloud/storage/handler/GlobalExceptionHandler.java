package com.cloud.storage.handler;

import com.cloud.storage.dto.response.ErrorResponse;
import com.cloud.storage.handler.exception.UserNotFoundException;
import com.cloud.storage.handler.exception.auth.UsernameAlredyExistException;
import com.cloud.storage.handler.exception.resourse.InvalidPathException;
import com.cloud.storage.handler.exception.resourse.ResourceAlreadyExistsException;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            InvalidPathException ex,
            WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                ex.getMessage(),
                request
        );
    }

    //401
    @ExceptionHandler({
            AuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthentication(
            Exception ex,
            WebRequest request) {

        log.warn("Authentication error: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid credentials or authentication required",
                request
        );
    }

    //404
    @ExceptionHandler({
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundResource(
            Exception ex,
            WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            UserNotFoundException.class,
    })
    public ResponseEntity<ErrorResponse> handleNotFoundUser(
            UserNotFoundException ex,
            WebRequest request) {

        log.warn("Username not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
    }

    //409
    @ExceptionHandler({
            UsernameAlredyExistException.class,
    })
    public ResponseEntity<ErrorResponse> handleNotFoundUsername(
            UsernameAlredyExistException ex,
            WebRequest request) {

        log.warn("Username is exist: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            ResourceAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundRecorcename(
            ResourceAlreadyExistsException ex,
            WebRequest request) {

        log.warn("Resource is exist: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                ex.getMessage(),
                request
        );
    }

    //500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request
        );
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}