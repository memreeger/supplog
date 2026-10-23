package com.supplog.service.notification;

public enum ReminderPhase {
    // Dozun saatinden önce: "vakti yaklaşıyor".
    PRE,
    // Saati geçti ama henüz kaçırılmadı: "kaçırıyorsun".
    LATE,
    // missedAt geçti ve doz kaçırıldı olarak işaretlendi.
    MISSED
}
