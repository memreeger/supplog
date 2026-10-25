package com.supplog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Hangi dozun hangi dilimi kime işlendi. Satırlar yalnızca {@code NotificationDispatchLogRepository.claim}
 * ile eklenir; benzersiz kısıt çift işlemeyi engeller.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_dispatch_log")
public class NotificationDispatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, length = 20)
    private String slot;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
