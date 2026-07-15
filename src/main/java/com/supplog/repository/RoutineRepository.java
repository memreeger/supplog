package com.supplog.repository;

import com.supplog.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserId(Long userId);

    List<Routine> findAllByIsDeletedFalse();

    List<Routine> findAllByIsDeletedTrue();

    List<Routine> findAllBySupplementId(Long userId);

    List<Routine> findAllByUserIdAndIsDeletedFalse(Long userId);

    Optional<Routine> findByIdAndUserIdAndIsDeletedFalse(Long routineId, Long userId);

    boolean existsBySupplementIdAndUserIdAndDeletedFalse(Long supplementId, Long userId);

    @Modifying
    @Query("""
    UPDATE Routine r
    SET r.isDeleted = true
    WHERE r.user.id = :userId
      AND r.isDeleted = false
""")
    void softDeleteAllByUserId(@Param("userId") Long userId);


}
