package com.supplog.service.admin.audit.impl;

import com.supplog.dto.admin.audit.AdminAuditResponseDto;
import com.supplog.entity.AdminAuditLog;
import com.supplog.repository.AdminAuditLogRepository;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.util.InputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {
    private final AdminAuditLogRepository repository;

    public AdminAuditServiceImpl(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(Long adminUserId, String action, String resourceType, Long resourceId,
            String oldValue, String newValue, String reason,
            boolean successful, String errorMessage) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(limit(InputNormalizer.trim(reason), 500, "unspecified"));
        log.setSuccessful(successful);
        log.setErrorMessage(limit(errorMessage, 500, null));
        log.setCreatedAt(LocalDateTime.now(java.time.Clock.systemUTC()));
        repository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAuditResponseDto> getRecent() {
        return repository.findTop20ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAuditResponseDto> getRecentForResource(String resourceType, Long resourceId) {
        return repository.findTop20ByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId)
                .stream().map(this::toDto).toList();
    }

    private AdminAuditResponseDto toDto(AdminAuditLog log) {
        return new AdminAuditResponseDto(log.getId(), log.getAdminUserId(), log.getAction(),
                log.getResourceType(), log.getResourceId(), log.getOldValue(), log.getNewValue(),
                log.getReason(), log.isSuccessful(), log.getErrorMessage(), log.getCreatedAt());
    }

    private String limit(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
