package com.supplog.exception;

// İş kuralı gereği çok sık yapılan istek: 429 döner, messageKey ile istemci kararını verir.
public class RateLimitedException extends RuntimeException {

    public RateLimitedException(String messageKey) {
        super(messageKey);
    }
}
