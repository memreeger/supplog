package com.supplog.service.notification;

/**
 * Kullanıcının oturumları kapatıldı (tüm cihazlardan çıkış, şifre sıfırlama, hesap pasifleştirme):
 * artık bu kullanıcıya push gönderilmemeli.
 */
public record UserSessionsRevokedEvent(Long userId) {
}
