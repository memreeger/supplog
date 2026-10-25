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
@Table(name = "nudges")
public class Nudge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_user_id", nullable = false, updatable = false)
    private Long senderUserId;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private Long recipientUserId;

    @Column(name = "routine_id", nullable = false, updatable = false)
    private Long routineId;

    // Ya hazır şablon anahtarı ya serbest metin dolu olur (veritabanı CHECK kısıtı).
    @Column(name = "template_key", length = 40, updatable = false)
    private String templateKey;

    @Column(length = 280, updatable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
