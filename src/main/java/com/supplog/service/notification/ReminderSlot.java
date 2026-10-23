package com.supplog.service.notification;

import java.time.Instant;

/**
 * Bir dozun bildirim zaman çizelgesindeki tek bir dilim.
 *
 * @param code    dispatch log'daki kimlik, örn. "PRE-10", "LATE-20", "MISSED"
 * @param at      dilimin tam zamanı
 * @param minutes PRE'de dozun saatine, LATE'te missedAt'e kalan dakika (MISSED'de 0)
 */
public record ReminderSlot(ReminderPhase phase, String code, Instant at, int minutes) {

    // Dispatch log'daki "kind" sütunu: PRE ve LATE aynı hatırlatma ailesidir.
    public String kind() {
        return phase == ReminderPhase.MISSED ? "MISSED" : "REMINDER";
    }
}
