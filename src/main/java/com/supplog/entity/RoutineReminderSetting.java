package com.supplog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "routine_reminder_settings")
public class RoutineReminderSetting {

    public static final int DEFAULT_LEAD_MINUTES = 30;

    @Id
    @Column(name = "routine_id")
    private Long routineId;

    @Column(nullable = false)
    private boolean enabled = true;

    // Dozun saatinden kaç dakika önce hatırlatmaya başlansın (10, 20, 30 veya 60).
    @Column(name = "lead_minutes", nullable = false)
    private int leadMinutes = DEFAULT_LEAD_MINUTES;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RoutineReminderSetting(Long routineId) {
        this.routineId = routineId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
