package com.supplog.repository;

import com.supplog.entity.NudgeMute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NudgeMuteRepository extends JpaRepository<NudgeMute, Long> {

    boolean existsByRecipientUserIdAndSenderUserId(Long recipientUserId, Long senderUserId);

    List<NudgeMute> findAllByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    long deleteByRecipientUserIdAndSenderUserId(Long recipientUserId, Long senderUserId);
}
