package com.supplog.repository;

import com.supplog.entity.Nudge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface NudgeRepository extends JpaRepository<Nudge, Long> {

    long countBySenderUserIdAndRoutineIdAndCreatedAtAfter(Long senderUserId, Long routineId, Instant after);

    long countBySenderUserIdAndRecipientUserIdAndCreatedAtAfter(Long senderUserId, Long recipientUserId, Instant after);
}
