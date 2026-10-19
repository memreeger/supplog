package com.supplog.exception;

import lombok.Getter;

// 401 ile sonuçlanan token hataları (refresh token vb.).
@Getter
public class AuthTokenException extends RuntimeException {

    private final String code;

    public AuthTokenException(String code, String messageKey) {
        super(messageKey);
        this.code = code;
    }
}
