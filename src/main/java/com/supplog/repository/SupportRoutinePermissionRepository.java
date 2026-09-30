package com.supplog.repository;

import com.supplog.entity.SupportRoutinePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface SupportRoutinePermissionRepository
        extends JpaRepository<SupportRoutinePermission, Long> {

    boolean existsBySupportRelationshipIdAndRoutineId(
            Long relationshipId,
            Long routineId
    );

    List<SupportRoutinePermission> findAllBySupportRelationshipId(
            Long relationshipId
    );

    @EntityGraph(attributePaths = "routine")
    List<SupportRoutinePermission> findAllBySupportRelationshipIdIn(
            Collection<Long> relationshipIds
    );

    @EntityGraph(attributePaths = {
            "routine",
            "routine.supplement",
            "routine.daysOfWeek"
    })
    List<SupportRoutinePermission>
    findAllBySupportRelationshipIdAndRoutineIsDeletedFalse(
            Long relationshipId
    );

    void deleteBySupportRelationshipIdAndRoutineId(
            Long relationshipId,
            Long routineId
    );

    void deleteAllBySupportRelationshipId(
            Long relationshipId
    );

    void deleteAllByRoutineId(Long routineId);
}
