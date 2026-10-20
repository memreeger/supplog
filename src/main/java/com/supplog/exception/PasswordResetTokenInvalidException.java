package com.supplog.exception;

// Süresi dolmuş, kullanılmış veya bilinmeyen şifre sıfırlama token'ı (400).
public class PasswordResetTokenInvalidException extends RuntimeException {

    public PasswordResetTokenInvalidException() {
        super("auth.password.reset.token.invalid");
    }
}
