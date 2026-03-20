package com.lendbridge.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LendbridgeException extends RuntimeException {

    private final HttpStatus status;

    public LendbridgeException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static LendbridgeException badRequest(String msg) {
        return new LendbridgeException(msg, HttpStatus.BAD_REQUEST);
    }

    public static LendbridgeException notFound(String msg) {
        return new LendbridgeException(msg, HttpStatus.NOT_FOUND);
    }

    public static LendbridgeException conflict(String msg) {
        return new LendbridgeException(msg, HttpStatus.CONFLICT);
    }

    public static LendbridgeException forbidden(String msg) {
        return new LendbridgeException(msg, HttpStatus.FORBIDDEN);
    }

    public static LendbridgeException unauthorized(String msg) {
        return new LendbridgeException(msg, HttpStatus.UNAUTHORIZED);
    }
}
