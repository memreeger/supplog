package com.supplog.service.admin.adminSupportService;

import com.supplog.dto.admin.support.AdminSupportResponseDto;
import com.supplog.entity.SupportRelationship;
import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;
import com.supplog.exception.BusinessException;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.repository.SupportRoutinePermissionRepository;
import com.supplog.service.admin.audit.AdminAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminSupportService {

    private final SupportRelationshipRepository relationshipRepository;
    private final SupportRoutinePermissionRepository permissionRepository;
    private final AdminAuditService auditService;

    public AdminSupportService(
            SupportRelationshipRepository relationshipRepository,
            SupportRoutinePermissionRepository permissionRepository,
            AdminAuditService auditService
    ) {
        this.relationshipRepository = relationshipRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    public List<AdminSupportResponseDto> search(SupportStatus status, Long userId) {
        return relationshipRepository.searchAdminRelationships(status, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void revoke(Long adminId, Long relationshipId, String reason) {
        SupportRelationship relationship = relationshipRepository
                .findById(relationshipId)
                .orElseThrow(() -> new BusinessException(
                        "support.relationship.not.available"
                ));

        if (relationship.getStatus() != SupportStatus.ACCEPTED) {
            throw new BusinessException(
                    "admin.support.relationship.not.active"
            );
        }

        SupportStatus previousStatus = relationship.getStatus();
        permissionRepository.deleteAllBySupportRelationshipId(relationshipId);
        relationship.setStatus(SupportStatus.REVOKED);
        relationship.setAccessScope(SupportAccessScope.SELECTED_ROUTINES);
        relationship.setRevokedAt(LocalDateTime.now());

        auditService.record(
                adminId,
                "SUPPORT_RELATIONSHIP_REVOKED",
                "SUPPORT_RELATIONSHIP",
                relationshipId,
                previousStatus.name(),
                SupportStatus.REVOKED.name(),
                reason,
                true,
                null
        );
    }

    public AdminSupportResponseDto toDto(SupportRelationship relationship) {
        Set<Long> routineIds = permissionRepository
                .findAllBySupportRelationshipId(relationship.getId())
                .stream()
                .map(permission -> permission.getRoutine().getId())
                .collect(Collectors.toSet());

        return new AdminSupportResponseDto(
                relationship.getId(),
                relationship.getSupportedUser().getId(),
                relationship.getSupportedUser().getUsername(),
                relationship.getSupporter().getId(),
                relationship.getSupporter().getUsername(),
                relationship.getStatus(),
                relationship.getAccessScope(),
                routineIds,
                relationship.getRequestedAt(),
                relationship.getRespondedAt(),
                relationship.getRevokedAt()
        );
    }
}
