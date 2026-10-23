package com.supplog.service.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Saf zaman hesabı (veritabanı yok, saat dışarıdan gelir): bir dozun hangi anlarda bildirim alacağını
 * ve "şimdi" hangi dilimde olduğumuzu söyler.
 * <p>
 * Dozun saatinden önce: {@code lead}'den başlayıp 10 dakikalık adımlarla, son 5 dakikada her dakika.
 * Saatten sonra: 0, 10, 20… dakikada, kaçırılmadan önceki son 5 dakikada her dakika.
 */
public final class ReminderSlots {

    static final int STEP_MINUTES = 10;
    static final int LAST_WINDOW_MINUTES = 5;
    // Zamanlayıcı dakikada bir çalışır; kaçan bir tick'i bu kadar süre geriden yakalarız.
    static final Duration TOLERANCE = Duration.ofMinutes(2);

    private ReminderSlots() {
    }

    public static List<ReminderSlot> slotsFor(Instant scheduledAt, Instant missedAt, int leadMinutes) {
        List<ReminderSlot> slots = new ArrayList<>();

        for (int before = leadMinutes; before > LAST_WINDOW_MINUTES; before -= STEP_MINUTES) {
            slots.add(pre(scheduledAt, before));
        }
        for (int before = LAST_WINDOW_MINUTES; before >= 1; before--) {
            slots.add(pre(scheduledAt, before));
        }

        int grace = (int) Duration.between(scheduledAt, missedAt).toMinutes();
        for (int elapsed = 0; elapsed < grace - LAST_WINDOW_MINUTES; elapsed += STEP_MINUTES) {
            slots.add(late(scheduledAt, elapsed, grace));
        }
        for (int remaining = Math.min(LAST_WINDOW_MINUTES, grace); remaining >= 1; remaining--) {
            slots.add(late(scheduledAt, grace - remaining, grace));
        }

        return slots;
    }

    /**
     * Destekçiye giden uyarılar (D4): dozun vakti geldi, kaçırılmasına 5 dakika kala. "Kaçırıldı" ayrıca
     * {@link #missedSlot} ile gönderilir. Kod, bildirimin {@code phase} değeri olarak da kullanılır.
     */
    public static List<ReminderSlot> supporterSlotsFor(Instant scheduledAt, Instant missedAt) {
        int grace = (int) Duration.between(scheduledAt, missedAt).toMinutes();
        List<ReminderSlot> slots = new ArrayList<>();
        slots.add(new ReminderSlot(ReminderPhase.LATE, "ON_TIME", scheduledAt, grace));
        if (grace > LAST_WINDOW_MINUTES) {
            slots.add(new ReminderSlot(ReminderPhase.LATE, "LAST_CALL",
                    missedAt.minus(Duration.ofMinutes(LAST_WINDOW_MINUTES)), LAST_WINDOW_MINUTES));
        }
        return slots;
    }

    public static ReminderSlot missedSlot(Instant missedAt) {
        return new ReminderSlot(ReminderPhase.MISSED, "MISSED", missedAt, 0);
    }

    // En son geçmiş dilim; ama sadece TOLERANCE içindeyse (çok eski bir hatırlatmayı göndermek anlamsız).
    public static Optional<ReminderSlot> currentSlot(List<ReminderSlot> slots, Instant now) {
        ReminderSlot latest = null;
        for (ReminderSlot slot : slots) {
            if (!slot.at().isAfter(now) && (latest == null || slot.at().isAfter(latest.at()))) {
                latest = slot;
            }
        }
        if (latest == null || Duration.between(latest.at(), now).compareTo(TOLERANCE) >= 0) {
            return Optional.empty();
        }
        return Optional.of(latest);
    }

    private static ReminderSlot pre(Instant scheduledAt, int minutesBefore) {
        return new ReminderSlot(ReminderPhase.PRE, "PRE-" + minutesBefore,
                scheduledAt.minus(Duration.ofMinutes(minutesBefore)), minutesBefore);
    }

    private static ReminderSlot late(Instant scheduledAt, int elapsed, int grace) {
        return new ReminderSlot(ReminderPhase.LATE, "LATE-" + elapsed,
                scheduledAt.plus(Duration.ofMinutes(elapsed)), grace - elapsed);
    }
}
