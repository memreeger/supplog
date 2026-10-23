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
@Table(name = "nudge_mutes")
public class NudgeMute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private Long recipientUserId;

    @Column(name = "sender_user_id", nullable = false, updatable = false)
    private Long senderUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NudgeMute(Long recipientUserId, Long senderUserId) {
        this.recipientUserId = recipientUserId;
        this.senderUserId = senderUserId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
