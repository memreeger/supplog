package com.supplog.exception;

public class BusinessException extends RuntimeException {
    private final Object[] args;

    public BusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
