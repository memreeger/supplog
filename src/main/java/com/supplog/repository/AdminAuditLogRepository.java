package com.supplog.repository;

import com.supplog.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findTop20ByOrderByCreatedAtDesc();

    List<AdminAuditLog> findTop20ByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType,
            Long resourceId
    );
}
