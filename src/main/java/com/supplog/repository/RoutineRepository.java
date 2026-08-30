package com.supplog.repository;

import com.supplog.entity.Routine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    Optional<Routine> findByIdAndUserIdAndIsDeletedFalse(Long routineId, Long userId);

    boolean existsBySupplementIdAndUserIdAndDeletedFalse(Long supplementId, Long userId);

    boolean existsBySupplementIdAndDeletedFalse(Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
                UPDATE Routine r
                SET r.isDeleted = true,
                r.updatedAt = CURRENT_TIMESTAMP
                WHERE r.user.id = :userId
                  AND r.isDeleted = false
            """)
    void softDeleteAllByUserId(@Param("userId") Long userId);


}
