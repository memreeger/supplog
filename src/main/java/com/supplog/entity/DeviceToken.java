package com.supplog.entity;

import com.supplog.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Satırlar {@code DeviceTokenRepository.upsert} ile yazılır (aynı token'ın yarışan kayıtları için
 * tek SQL); entity okuma içindir. Token değeri bir sırdır: loglanmaz.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "push_token", nullable = false, length = 512, updatable = false)
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(length = 10)
    private String locale;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
