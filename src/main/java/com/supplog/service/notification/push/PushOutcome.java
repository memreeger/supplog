package com.supplog.service.notification.push;

public enum PushOutcome {
    SENT,
    // Token artık geçerli değil (uygulama silindi, token yenilendi…): kaydı silinmeli.
    INVALID_TOKEN,
    // Geçici ya da bilinmeyen hata: token korunur, sonraki bildirimde tekrar denenir.
    FAILED
}
