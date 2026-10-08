package com.project.BharatConnect.service.minimoth;

/**
 * Thrown when the MiniMoth API returns an error or is unreachable.
 */
public class MiniMothException extends RuntimeException {

    public MiniMothException(String message) {
        super(message);
    }

    public MiniMothException(String message, Throwable cause) {
        super(message, cause);
    }
}
