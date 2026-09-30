package com.supplog.service.admin.adminSupportService.impl;

import com.supplog.dto.admin.support.AdminSupportResponseDto;
import com.supplog.entity.SupportRelationship;
import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;
import com.supplog.exception.BusinessException;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.repository.SupportRoutinePermissionRepository;
import com.supplog.service.admin.adminSupportService.AdminSupportService;
import com.supplog.service.admin.audit.AdminAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminSupportServiceImpl implements AdminSupportService {
    private final SupportRelationshipRepository relationshipRepository;
    private final SupportRoutinePermissionRepository permissionRepository;
    private final AdminAuditService auditService;

    public AdminSupportServiceImpl(SupportRelationshipRepository relationshipRepository,
            SupportRoutinePermissionRepository permissionRepository,
            AdminAuditService auditService) {
        this.relationshipRepository = relationshipRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Override
    public Page<AdminSupportResponseDto> search(
            SupportStatus status,
            Long userId,
            Pageable pageable
    ) {
        Page<SupportRelationship> relationships =
                relationshipRepository.searchAdminRelationships(status, userId, pageable);

        Map<Long, Set<Long>> routineIdsByRelationship = relationships.isEmpty()
                ? Collections.emptyMap()
                : permissionRepository
                        .findAllBySupportRelationshipIdIn(
                                relationships.stream().map(SupportRelationship::getId).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                permission -> permission.getSupportRelationship().getId(),
                                Collectors.mapping(
                                        permission -> permission.getRoutine().getId(),
                                        Collectors.toSet()
                                )
                        ));

        return relationships.map(relationship -> toDto(
                relationship,
                routineIdsByRelationship.getOrDefault(
                        relationship.getId(),
                        Collections.emptySet()
                )
        ));
    }

    @Override
    @Transactional
    public void revoke(Long adminId, Long relationshipId, String reason) {
        SupportRelationship relationship = relationshipRepository.findByIdForUpdate(relationshipId)
                .orElseThrow(() -> new BusinessException("support.relationship.not.available"));
        if (relationship.getStatus() != SupportStatus.ACCEPTED) {
            throw new BusinessException("admin.support.relationship.not.active");
        }
        SupportStatus previousStatus = relationship.getStatus();
        permissionRepository.deleteAllBySupportRelationshipId(relationshipId);
        relationship.setStatus(SupportStatus.REVOKED);
        relationship.setAccessScope(SupportAccessScope.SELECTED_ROUTINES);
        relationship.setRevokedAt(LocalDateTime.now());
        auditService.record(adminId, "SUPPORT_RELATIONSHIP_REVOKED", "SUPPORT_RELATIONSHIP",
                relationshipId, previousStatus.name(), SupportStatus.REVOKED.name(),
                reason, true, null);
    }

    private AdminSupportResponseDto toDto(
            SupportRelationship relationship,
            Set<Long> routineIds
    ) {
        return new AdminSupportResponseDto(relationship.getId(),
                relationship.getSupportedUser().getId(), relationship.getSupportedUser().getUsername(),
                relationship.getSupporter().getId(), relationship.getSupporter().getUsername(),
                relationship.getStatus(), relationship.getAccessScope(), routineIds,
                relationship.getRequestedAt(), relationship.getRespondedAt(), relationship.getRevokedAt());
    }
}
