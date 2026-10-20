package com.supplog.repository;

import com.supplog.entity.RefreshToken;
import com.supplog.enums.RefreshTokenRevokeReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rt from RefreshToken rt
            join fetch rt.user
            where rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revokedAt = :now, rt.revokeReason = :reason
            where rt.familyId = :familyId and rt.revokedAt is null
            """)
    int revokeFamily(
            @Param("familyId") UUID familyId,
            @Param("reason") RefreshTokenRevokeReason reason,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revokedAt = :now, rt.revokeReason = :reason
            where rt.user.id = :userId and rt.revokedAt is null
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("reason") RefreshTokenRevokeReason reason,
            @Param("now") Instant now
    );

    // replaced_by_id FK ON DELETE SET NULL olduğu için zincir ortasından silmek güvenli.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from refresh_tokens
            where expires_at < :threshold
               or revoked_at < :threshold
            """, nativeQuery = true)
    int deleteExpiredOrRevokedBefore(@Param("threshold") Instant threshold);
}
