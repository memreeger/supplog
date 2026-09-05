package com.supplog.repository;

import com.supplog.entity.SupportRelationship;
import com.supplog.enums.SupportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportRelationshipRepository
        extends JpaRepository<SupportRelationship, Long> {

    Optional<SupportRelationship> findBySupportedUserIdAndSupporterId(
            Long supportedUserId,
            Long supporterId
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
}