package com.supplog.repository;

import com.supplog.entity.Routine;
import com.supplog.enums.Frequency;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAll();

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllByIsDeletedFalse();

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllByIsDeletedTrue();

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllBySupplementId(Long supplementId);

    //EntityGraph sayesinde "Routine'leri getirirken supplement ilişkisini de bu sorgu kapsamında yükle demiş oluyoruz".
    // N + 1 problemi yaşadığım için bunu kullandım!!!
    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllByUserIdAndIsDeletedFalse(Long userId);

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    List<Routine> findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(Long userId);

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    Optional<Routine> findByIdAndUserIdAndIsDeletedFalse(Long routineId, Long userId);

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek"})
    Optional<Routine> findByIdAndUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(
            Long routineId,
            Long userId
    );

    boolean existsBySupplementIdAndUserIdAndDeletedFalse(Long supplementId, Long userId);

    boolean existsBySupplementIdAndDeletedFalse(Long id);

    @Query("""
            SELECT r.id
            FROM Routine r
            WHERE r.supplement.id = :supplementId
              AND r.isDeleted = false
            ORDER BY r.id
            """)
    List<Long> findActiveRoutineIdsBySupplementId(
            @Param("supplementId") Long supplementId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
                UPDATE Routine r
                SET r.isDeleted = true,
                r.updatedAt = CURRENT_TIMESTAMP
                WHERE r.user.id = :userId
                  AND r.isDeleted = false
            """)
    void softDeleteAllByUserId(@Param("userId") Long userId);

    List<Routine> findAllByIdInAndUserIdAndIsDeletedFalse(
            Collection<Long> routineIds,
            Long userId
    );

    long countByIsDeletedFalse();

    long countByIsDeletedTrue();

    @EntityGraph(attributePaths = {"supplement", "daysOfWeek", "user"})
    @Query("""
            SELECT r
            FROM Routine r
            WHERE (:userId IS NULL OR r.user.id = :userId)
              AND (:supplementId IS NULL OR r.supplement.id = :supplementId)
              AND (:frequency IS NULL OR r.frequency = :frequency)
              AND (:active IS NULL
                   OR (:active = true AND r.isDeleted = false)
                   OR (:active = false AND r.isDeleted = true))
            ORDER BY r.createdAt DESC
            """)
    List<Routine> searchAdminRoutines(
            @Param("userId") Long userId,
            @Param("supplementId") Long supplementId,
            @Param("frequency") Frequency frequency,
            @Param("active") Boolean active
    );


}
