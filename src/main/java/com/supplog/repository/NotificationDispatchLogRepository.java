package com.supplog.repository;

import com.supplog.entity.NotificationDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationDispatchLogRepository extends JpaRepository<NotificationDispatchLog, Long> {

    /**
     * Dilimi sahiplenir: 1 dönerse bu çağrı ekledi (gönderim sırası bizde), 0 dönerse daha önce işlenmişti.
     * Tek SQL olduğu için iki instance aynı anda denese bile yalnızca biri kazanır.
     */
    @Modifying
    @Query(value = """
            INSERT INTO notification_dispatch_log
                (execution_id, recipient_user_id, kind, slot, sent_at)
            VALUES (:executionId, :recipientUserId, :kind, :slot, :now)
            ON CONFLICT (execution_id, recipient_user_id, kind, slot) DO NOTHING
            """, nativeQuery = true)
    int claim(
            @Param("executionId") Long executionId,
            @Param("recipientUserId") Long recipientUserId,
            @Param("kind") String kind,
            @Param("slot") String slot,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NotificationDispatchLog l where l.sentAt < :threshold")
    int deleteSentBefore(@Param("threshold") Instant threshold);
}
