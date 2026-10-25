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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :now, n.updatedAt = :now
            where n.recipientUserId = :userId and n.readAt is null and n.resolvedAt is null
            """)
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.createdAt < :threshold")
    int deleteCreatedBefore(@Param("threshold") Instant threshold);
}
