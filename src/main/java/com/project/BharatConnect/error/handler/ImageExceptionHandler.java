package com.project.BharatConnect.error.handler;
import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import com.project.BharatConnect.error.exception.MediaProcessingException;
import com.project.BharatConnect.error.exception.MediaTooLargeException;
import com.project.BharatConnect.error.exception.InvalidMediaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class ImageExceptionHandler {

    private final ErrorBuilder errorBuilder;

    public ImageExceptionHandler(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    @ExceptionHandler(InvalidMediaException.class)
    public ResponseEntity<List<ApiError>> handleInvalidImage(
            InvalidMediaException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "INVALID_IMAGE",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MediaTooLargeException.class)
    public ResponseEntity<List<ApiError>> handleImageTooLarge(
            MediaTooLargeException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "IMAGE_TOO_LARGE",
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    @ExceptionHandler(MediaProcessingException.class)
    public ResponseEntity<List<ApiError>> handleImageProcessing(
            MediaProcessingException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "IMAGE_PROCESSING_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}
