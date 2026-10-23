package com.supplog.repository;

import com.supplog.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Çözülmüş (artık geçerli olmayan) bildirimler listede görünmez.
    Page<Notification> findByRecipientUserIdAndResolvedAtIsNull(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadAtIsNullAndResolvedAtIsNull(Long recipientUserId);

    // Sahiplik kontrolü sorgunun içinde: başkasının ID'si "bulunamadı" gibi davranır.
    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    Optional<Notification> findByRecipientUserIdAndDedupeKey(Long recipientUserId, String dedupeKey);

    // updatedAt'e dokunmayız: okundu işaretlemek satırı listede yukarı zıplatmamalı.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :now
            where n.recipientUserId = :userId and n.readAt is null and n.resolvedAt is null
            """)
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :now
            where n.id = :id and n.recipientUserId = :userId and n.readAt is null
            """)
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    // Yalnızca belirli türdeki bildirimleri çözer (örn. kaçırılınca sadece hatırlatma satırı).
    @Modifying(flushAutomatically = true)
    @Query("""
            update Notification n
            set n.resolvedAt = :now
            where n.executionId = :executionId and n.type = :type and n.resolvedAt is null
            """)
    int resolveByExecutionAndType(
            @Param("executionId") Long executionId,
            @Param("type") com.supplog.enums.NotificationType type,
            @Param("now") Instant now
    );

    // Destekçinin, sahibin artık izinli olmadığı rutinlerdeki uyarıları çözülür.
    @Modifying(flushAutomatically = true)
    @Query("""
            update Notification n
            set n.resolvedAt = :now
            where n.recipientUserId = :supporterId
              and n.actorUserId = :ownerId
              and n.type = com.supplog.enums.NotificationType.SUPPORT_ALERT
              and n.resolvedAt is null
              and (n.routineId is null or n.routineId not in :keepRoutineIds)
            """)
    int resolveSupportAlerts(
            @Param("supporterId") Long supporterId,
            @Param("ownerId") Long ownerId,
            @Param("keepRoutineIds") java.util.Collection<Long> keepRoutineIds,
            @Param("now") Instant now
    );

    // Çağıran transaction'ın entity'lerini bırakmak için clear yok (execution kaydı aynı transaction'da).
    @Modifying(flushAutomatically = true)
    @Query("""
            update Notification n
            set n.resolvedAt = :now
            where n.executionId = :executionId and n.resolvedAt is null
            """)
    int resolveByExecution(@Param("executionId") Long executionId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.createdAt < :threshold")
    int deleteCreatedBefore(@Param("threshold") Instant threshold);
}
