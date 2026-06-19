package com.supplog.exception;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter

@NoArgsConstructor
@ToString
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private Map<String, String> errors;

    public ErrorResponse(int status, String message, LocalDateTime timeStamp ){
        this(status, message, timeStamp, null);
    }

    public ErrorResponse(int status, String message, LocalDateTime timeStamp, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.timeStamp = timeStamp;
        this.errors = errors;

    }
}
