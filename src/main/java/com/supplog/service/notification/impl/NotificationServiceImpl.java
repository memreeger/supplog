package com.supplog.service.notification.impl;

import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.UnreadCountResponseDto;
import com.supplog.entity.Notification;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.NotificationRepository;
import com.supplog.service.notification.NotificationDraft;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
        // Son hareket önce: her yeni hatırlatma dilimi satırı yukarı taşır. Okundu işaretlemek
        // updatedAt'i değiştirmez (bkz. NotificationRepository.markRead), satır yerinde kalır.
        PageRequest page = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

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
        if (notificationRepository.findByIdAndRecipientUserId(notificationId, userId).isEmpty()) {
            throw new ResourceNotFoundException("notification.not.found");
        }
        notificationRepository.markRead(notificationId, userId, clock.instant());
    }

    @Override
    @Transactional
    public void upsertForExecution(NotificationDraft draft) {
        Notification notification = notificationRepository
                .findByRecipientUserIdAndDedupeKey(draft.recipientUserId(), draft.dedupeKey())
                .orElse(null);

        if (notification == null) {
            notification = new Notification();
            notification.setRecipientUserId(draft.recipientUserId());
            notification.setDedupeKey(draft.dedupeKey());
        } else if (notification.getResolvedAt() != null
                || !Objects.equals(notification.getParams().get("phase"), draft.params().get("phase"))) {
            // Çözülmüş (örn. geri alınan) ya da PRE→LATE'e geçen hatırlatma yeniden dikkat çekmeli.
            notification.setReadAt(null);
            notification.setResolvedAt(null);
        }

        notification.setType(draft.type());
        notification.setExecutionId(draft.executionId());
        notification.setRoutineId(draft.routineId());
        notification.setActorUserId(draft.actorUserId());
        notification.setNudgeId(draft.nudgeId());
        notification.setParams(new HashMap<>(draft.params()));
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void resolveForExecution(Long executionId) {
        notificationRepository.resolveByExecution(executionId, clock.instant());
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, clock.instant());
    }

    @Override
    @Transactional
    public void resolveForExecution(Long executionId, com.supplog.enums.NotificationType type) {
        notificationRepository.resolveByExecutionAndType(executionId, type, clock.instant());
    }

    @Override
    @Transactional
    public void resolveSupportAlerts(Long supporterUserId, Long ownerUserId, java.util.Collection<Long> keepRoutineIds) {
        // JPQL "not in ()" boş listede hata verir; geçerli hiçbir id'ye denk gelmeyen bir değer koyarız.
        java.util.Collection<Long> keep = keepRoutineIds.isEmpty() ? List.of(-1L) : keepRoutineIds;
        notificationRepository.resolveSupportAlerts(supporterUserId, ownerUserId, keep, clock.instant());
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
