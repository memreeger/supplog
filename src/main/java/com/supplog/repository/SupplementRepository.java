package com.supplog.repository;

import com.supplog.entity.Supplement;
import com.supplog.enums.RoutineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplementRepository extends JpaRepository<Supplement, Long> {
    List<Supplement> findAllByIsDeletedFalse();

    List<Supplement> findAllByIsDeletedTrue();

    List<Supplement> findAllByInsertedByUserIdAndIsDeletedFalse(Long userId);

    Optional<Supplement> findByIdAndInsertedByUserIdAndIsDeletedFalse(Long supplementId, Long userId);

    List<Supplement> findAllByInsertedByUserId(Long userId);

    @Modifying(flushAutomatically = true)
    @Query("""
                UPDATE Supplement s
                SET s.isDeleted = true,
                s.updatedAt = CURRENT_TIMESTAMP
                WHERE s.insertedByUser.id = :userId
                  AND s.isDeleted = false
            """)
    void softDeleteAllByUserId(@Param("userId") Long userId);

    boolean existsByIdAndIsDeletedFalse(Long id);

    long countByIsDeletedFalse();

    long countByIsDeletedTrue();

    @Query("""
            SELECT s
            FROM Supplement s
            WHERE (:userId IS NULL OR s.insertedByUser.id = :userId)
              AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:type IS NULL OR s.type = :type)
              AND (:active IS NULL
                   OR (:active = true AND s.isDeleted = false)
                   OR (:active = false AND s.isDeleted = true))
            ORDER BY s.createdAt DESC
            """)
    @EntityGraph(attributePaths = "insertedByUser")
    Page<Supplement> searchAdminSupplements(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("type") RoutineCategory type,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
