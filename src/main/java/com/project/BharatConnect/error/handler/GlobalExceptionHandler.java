package com.project.BharatConnect.error.handler;

import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final ErrorBuilder errorBuilder;

    public GlobalExceptionHandler(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiError> errors = new ArrayList<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(ApiError.builder()
                    .keyError(error.getField())
                    .valueError(error.getDefaultMessage())
                    .timeStamp(LocalDateTime.now())
                    .build());
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<ApiError>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {

        return errorBuilder.buildError(
                "Invalid request body. Please check the provided values.",
                "Invalid JSON",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<List<ApiError>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {

        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint",
                exception.getMethod()
        );

        return errorBuilder.buildError(
                message,
                "Method Not Allowed",
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<List<ApiError>> handleNoResourceFound(
            NoResourceFoundException exception) {

        return errorBuilder.buildError(
                "The requested endpoint was not found",
                "Endpoint Not Found",
                HttpStatus.NOT_FOUND
        );
    }
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<List<ApiError>> handleInvalidRequest(
            InvalidRequestException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "INVALID_REQUEST",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<ApiError>> handleAccessDeniedException(
            AccessDeniedException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Access Denied",
                HttpStatus.UNAUTHORIZED
        );
    }
}
