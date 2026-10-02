package com.project.BharatConnect.error.exception;

public class NestingLimitExceededException extends RuntimeException {
    public NestingLimitExceededException(String message) {
        super(message);
    }
}
