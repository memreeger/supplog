package com.supplog.repository;

import com.supplog.entity.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplementRepository extends JpaRepository<Supplement, Long> {
    Optional<Supplement> findSupplementByName(String name);

    List<Supplement> findAllByInsertedByUserIdAndIsDeletedFalse(Long userId);

    List<Supplement> findAllByInsertedByUserId(Long userId);
}
