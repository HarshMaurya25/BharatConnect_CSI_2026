package com.project.BharatConnect.error.handler;
import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import com.project.BharatConnect.error.exception.ImageProcessingException;
import com.project.BharatConnect.error.exception.ImageTooLargeException;
import com.project.BharatConnect.error.exception.InvalidImageException;
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

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<List<ApiError>> handleInvalidImage(
            InvalidImageException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "INVALID_IMAGE",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ImageTooLargeException.class)
    public ResponseEntity<List<ApiError>> handleImageTooLarge(
            ImageTooLargeException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "IMAGE_TOO_LARGE",
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<List<ApiError>> handleImageProcessing(
            ImageProcessingException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "IMAGE_PROCESSING_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}
