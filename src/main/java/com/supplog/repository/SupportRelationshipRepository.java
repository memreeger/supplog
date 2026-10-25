package com.supplog.repository;

import com.supplog.entity.SupportRelationship;
import com.supplog.enums.SupportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface SupportRelationshipRepository
        extends JpaRepository<SupportRelationship, Long> {

    Optional<SupportRelationship> findBySupportedUserIdAndSupporterId(
            Long supportedUserId,
            Long supporterId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM SupportRelationship sr
            WHERE sr.supportedUser.id = :supportedUserId
              AND sr.supporter.id = :supporterId
            """)
    Optional<SupportRelationship> findPairForUpdate(
            @Param("supportedUserId") Long supportedUserId,
            @Param("supporterId") Long supporterId
    );

    Optional<SupportRelationship> findByIdAndSupportedUserId(
            Long relationshipId,
            Long supportedUserId
    );

    Optional<SupportRelationship> findByIdAndSupporterId(
            Long relationshipId,
            Long supporterId
    );

    Optional<SupportRelationship> findByIdAndSupportedUserIdAndStatus(
            Long relationshipId,
            Long supportedUserId,
            SupportStatus status
    );

    Optional<SupportRelationship> findByIdAndSupporterIdAndStatus(
            Long relationshipId,
            Long supporterId,
            SupportStatus status
    );

    List<SupportRelationship>
    findAllBySupportedUserIdAndStatusOrderByRequestedAtDesc(
            Long supportedUserId,
            SupportStatus status
    );

    List<SupportRelationship>
    findAllBySupporterIdAndStatusOrderByRequestedAtDesc(
            Long supporterId,
            SupportStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM SupportRelationship sr
            WHERE sr.id = :relationshipId
              AND sr.supportedUser.id = :userId
              AND sr.status = :status
            """)
    Optional<SupportRelationship> findOwnerRelationshipForUpdate(
            @Param("relationshipId") Long relationshipId,
            @Param("userId") Long userId,
            @Param("status") SupportStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM SupportRelationship sr
            WHERE sr.id = :relationshipId
              AND sr.supporter.id = :userId
              AND sr.status = :status
            """)
    Optional<SupportRelationship> findSupporterRelationshipForUpdate(
            @Param("relationshipId") Long relationshipId,
            @Param("userId") Long userId,
            @Param("status") SupportStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM SupportRelationship sr WHERE sr.id = :relationshipId")
    Optional<SupportRelationship> findByIdForUpdate(
            @Param("relationshipId") Long relationshipId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sr FROM SupportRelationship sr
            WHERE (sr.supportedUser.id = :userId OR sr.supporter.id = :userId)
              AND sr.status IN :statuses
            ORDER BY sr.id
            """)
    List<SupportRelationship> findAllForUserAndStatusesForUpdate(
            @Param("userId") Long userId,
            @Param("statuses") Collection<SupportStatus> statuses
    );

    @EntityGraph(attributePaths = {"supportedUser", "supporter"})
    @Query("""
            SELECT sr
            FROM SupportRelationship sr
            WHERE (:status IS NULL OR sr.status = :status)
              AND (:userId IS NULL OR sr.supportedUser.id = :userId OR sr.supporter.id = :userId)
            ORDER BY sr.requestedAt DESC
            """)
    Page<SupportRelationship> searchAdminRelationships(
            @org.springframework.data.repository.query.Param("status") SupportStatus status,
            @org.springframework.data.repository.query.Param("userId") Long userId,
            Pageable pageable
    );

    long countByStatus(SupportStatus status);

    record SupporterTarget(Long routineId, Long supporterUserId) {
    }

    // Bu rutinlerin bildirim alabilecek destekçileri: ilişki ACCEPTED, destekçi aktif ve rutin izinli
    // (ALL_ROUTINES ya da SELECTED_ROUTINES + izin satırı). Tek sorgu, N+1 yok.
    @Query("""
            SELECT new com.supplog.repository.SupportRelationshipRepository$SupporterTarget(r.id, sr.supporter.id)
            FROM SupportRelationship sr, Routine r
            WHERE r.id IN :routineIds
              AND r.user.id = sr.supportedUser.id
              AND sr.status = :status
              AND sr.supporter.isDeleted = false
              AND (sr.accessScope = :allScope
                   OR EXISTS (
                        SELECT 1 FROM SupportRoutinePermission p
                        WHERE p.supportRelationship.id = sr.id
                          AND p.routine.id = r.id))
            """)
    List<SupporterTarget> findNotifiableSupporters(
            @Param("routineIds") Collection<Long> routineIds,
            @Param("status") SupportStatus status,
            @Param("allScope") com.supplog.enums.SupportAccessScope allScope
    );
}
