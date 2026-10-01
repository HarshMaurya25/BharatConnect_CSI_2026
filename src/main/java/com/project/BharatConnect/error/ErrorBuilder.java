package com.project.BharatConnect.error;

import com.project.BharatConnect.dto.exception.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ErrorBuilder {

    public ResponseEntity<List<ApiError>> buildError(
            String keyError,
            String valueError,
            HttpStatus status) {

        List<ApiError> error = List.of(
                ApiError.builder()
                        .keyError(keyError)
                        .valueError(valueError)
                        .timeStamp(LocalDateTime.now())
                        .build()
        );

        return new ResponseEntity<>(error, status);
    }
}
