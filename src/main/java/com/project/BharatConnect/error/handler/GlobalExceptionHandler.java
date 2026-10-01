package com.project.BharatConnect.error.handler;

import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
}
