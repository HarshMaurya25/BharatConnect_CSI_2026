package com.project.BharatConnect.error.exception;

public class ReplyLimitReachedException extends RuntimeException {
    public ReplyLimitReachedException(String message) {
        super(message);
    }
}
