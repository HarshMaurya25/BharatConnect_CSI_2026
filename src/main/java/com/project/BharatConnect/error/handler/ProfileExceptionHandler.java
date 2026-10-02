package com.project.BharatConnect.error.handler;

import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import com.project.BharatConnect.error.exception.UsernameNotUniqueException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.sasl.AuthenticationException;
import java.util.List;

@ControllerAdvice
public class ProfileExceptionHandler {

    private final ErrorBuilder errorBuilder;

    public ProfileExceptionHandler(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    @ExceptionHandler(UsernameNotUniqueException.class)
    public ResponseEntity<List<ApiError>> handleUsernameNotUnique(
            UsernameNotUniqueException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Authentication Failed",
                HttpStatus.BAD_REQUEST
        );
    }
}
