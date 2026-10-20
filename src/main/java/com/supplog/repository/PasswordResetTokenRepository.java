package com.supplog.repository;

import com.supplog.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select prt from PasswordResetToken prt
            join fetch prt.user
            where prt.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant after);

    // Yeni talep veya başarılı reset sonrası kullanıcının açık token'ları geçersiz olur.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PasswordResetToken prt
            set prt.usedAt = :now
            where prt.user.id = :userId and prt.usedAt is null
            """)
    int invalidateOpenTokens(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from password_reset_tokens
            where expires_at < :threshold
               or used_at < :threshold
            """, nativeQuery = true)
    int deleteExpiredOrUsedBefore(@Param("threshold") Instant threshold);
}
