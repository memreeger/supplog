package com.supplog.exception;

// İzin verilmeyen Origin'den gelen cookie tabanlı auth isteği (403).
public class OriginNotAllowedException extends RuntimeException {

    public OriginNotAllowedException() {
        super("auth.origin.not.allowed");
    }
}
