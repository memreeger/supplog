package com.supplog.exception;


import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final Object[] args;
    // Hata cevabına "details" olarak eklenir (ör. silmeyi engelleyen rutinler).
    private Map<String, Object> details;

    public BusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public BusinessException withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }

}
