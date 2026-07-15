package com.supplog.repository;

import com.supplog.entity.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<Supplement> findSupplementByName(String name);

    List<Supplement> findAllByInsertedByUserIdAndIsDeletedFalse(Long userId);

    Optional<Supplement> findByIdAndInsertedByUserIdAndIsDeletedFalse(Long supplementId, Long userId);

    List<Supplement> findAllByInsertedByUserId(Long userId);

    @Modifying
    @Query("""
    UPDATE Supplement s
    SET s.isDeleted = true
    WHERE s.insertedByUser.id = :userId
      AND s.isDeleted = false
""")
    void softDeleteAllByUserId(@Param("userId") Long userId);
}
