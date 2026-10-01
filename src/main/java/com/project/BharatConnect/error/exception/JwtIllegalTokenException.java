package com.project.BharatConnect.error.exception;

public class JwtIllegalTokenException extends RuntimeException{
    public JwtIllegalTokenException(String message) {
        super(message);
    }
}
