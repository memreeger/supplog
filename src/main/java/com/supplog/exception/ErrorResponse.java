package com.supplog.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ErrorResponse {

    private int status;
    private String code;
    private String message;
    private LocalDateTime timeStamp;
    private Map<String, String> errors;

    public ErrorResponse(int status, String code, String message, LocalDateTime timeStamp) {
        this(status, code, message, timeStamp, null);
    }

    public ErrorResponse(int status, String code, String message, LocalDateTime timeStamp, Map<String, String> errors) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timeStamp = timeStamp;
        this.errors = errors;
    }
}