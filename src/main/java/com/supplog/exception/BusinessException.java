package com.supplog.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Object[] args;

    public BusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

}
