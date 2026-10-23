package com.supplog.entity;

import com.supplog.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // İlişki yerine düz ID: çan listesi bu satırları çok okur, User yüklemek gereksiz.
    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    // Aynı olayın tekrar satır üretmesini önler; (recipient_user_id, dedupe_key) benzersizdir.
    @Column(name = "dedupe_key", nullable = false, length = 100, updatable = false)
    private String dedupeKey;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "routine_id")
    private Long routineId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "nudge_id")
    private Long nudgeId;

    // Metin değil veri: istemci type + params ile kendi dilinde çizer.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> params = new HashMap<>();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
