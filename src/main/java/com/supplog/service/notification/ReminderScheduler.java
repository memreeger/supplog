package com.supplog.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dakikada bir hatırlatma turu başlatır. {@code app.notifications.enabled=false} iken hiçbir şey yapmaz,
 * böylece özellik canlıya kapalı çıkar ve tek bayrakla açılır/kapanır.
 */
@Slf4j
@Component
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final boolean enabled;

    public ReminderScheduler(
            ReminderService reminderService,
            @Value("${app.notifications.enabled:false}") boolean enabled
    ) {
        this.reminderService = reminderService;
        this.enabled = enabled;
    }

    @Scheduled(
            fixedDelayString = "${app.notifications.reminder-interval-ms:60000}",
            initialDelayString = "${app.notifications.reminder-initial-delay-ms:30000}"
    )
    public void tick() {
        if (!enabled) {
            return;
        }
        try {
            reminderService.processDue();
        } catch (Exception e) {
            log.error("Reminder tick failed", e);
        }
    }
}
