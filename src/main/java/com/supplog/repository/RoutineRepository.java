package com.supplog.repository;

import com.supplog.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserId(Long userId);

    List<Routine> findAllByUserIdAndIsDeletedFalse(Long userId);

    Optional<Routine> findByIdAndIsDeletedFalse(Long id);


}
