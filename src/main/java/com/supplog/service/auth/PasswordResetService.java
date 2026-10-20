package com.supplog.service.auth;

public interface PasswordResetService {

    // Kullanıcı var/yok fark etmeksizin sessiz çalışır (hesap varlığı sızdırılmaz).
    void requestReset(String email, String requestIp);

    // Başarılı reset tüm oturumları kapatır (tokenVersion++).
    void resetPassword(String rawToken, String newPassword, String confirmPassword);

    void deleteExpiredTokens();
}
