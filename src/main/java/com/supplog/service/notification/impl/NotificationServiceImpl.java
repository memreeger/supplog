package com.supplog.service.notification.impl;

import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.UnreadCountResponseDto;
import com.supplog.entity.Notification;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.NotificationRepository;
import com.supplog.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final Duration retention;

    @Autowired
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            @Value("${app.notifications.retention-days:90}") long retentionDays
    ) {
        this(notificationRepository, Clock.systemUTC(), Duration.ofDays(retentionDays));
    }

    // Testlerde saat enjekte edilebilsin diye.
    NotificationServiceImpl(NotificationRepository notificationRepository, Clock clock, Duration retention) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.retention = retention;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> list(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        // createdAt'e göre sıralarız: okundu işaretlemek (updatedAt değişir) satırı yukarı zıplatmasın.
        PageRequest page = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        return notificationRepository.findByRecipientUserIdAndResolvedAtIsNull(userId, page)
                .map(NotificationResponseDto::from)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponseDto unreadCount(Long userId) {
        return new UnreadCountResponseDto(
                notificationRepository.countByRecipientUserIdAndReadAtIsNullAndResolvedAtIsNull(userId)
        );
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("notification.not.found"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, clock.instant());
    }

    @Override
    @Scheduled(cron = "${app.notifications.cleanup-cron:0 15 4 * * *}")
    @Transactional
    public void deleteOldNotifications() {
        Instant threshold = clock.instant().minus(retention);
        int deleted = notificationRepository.deleteCreatedBefore(threshold);
        log.info("Notification cleanup finished, deleted={}", deleted);
    }
}
