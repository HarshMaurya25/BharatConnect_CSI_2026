package com.project.BharatConnect.error.handler;

import com.project.BharatConnect.dto.exception.ApiError;
import com.project.BharatConnect.error.ErrorBuilder;
import com.project.BharatConnect.error.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.sasl.AuthenticationException;
import java.util.List;

@ControllerAdvice
public class AuthenticationExceptionHandler {

    private final ErrorBuilder errorBuilder;

    public AuthenticationExceptionHandler(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    @ExceptionHandler(JwtIllegalTokenException.class)
    public ResponseEntity<List<ApiError>> handleJwtIllegalTokenException(
            JwtIllegalTokenException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Illegal Token",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationException(
            AuthenticationException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED
        );
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<List<ApiError>> handleInvalidCredential(
            InvalidCredentialsException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Invalid Credential",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<ApiError>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return errorBuilder.buildError(
                exception.getMessage(),
                "Access Denied",
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleUsernameNotFound(
            UsernameNotFoundException exception) {

        return errorBuilder.buildError(
                exception.getMessage(),
                "User Not Found",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<List<ApiError>> handleDisabled(
            DisabledException exception) {

        return errorBuilder.buildError(
                "User account is disabled",
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(OtpAlreadySentException.class)
    public ResponseEntity<List<ApiError>> handleOtpAlreadySent(
            OtpAlreadySentException exception) {

        return errorBuilder.buildError(
                exception.getMessage(),
                "OTP Already Sent",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(OtpExpireException.class)
    public ResponseEntity<List<ApiError>> handleOtpExpire(
            OtpExpireException exception) {

        return errorBuilder.buildError(
                exception.getMessage(),
                "OTP Expire",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<List<ApiError>> handleUserAlreadyExist(
            UserAlreadyExistException exception) {

        return errorBuilder.buildError(
                exception.getMessage(),
                "User already exists",
                HttpStatus.CONFLICT
        );
    }


}
