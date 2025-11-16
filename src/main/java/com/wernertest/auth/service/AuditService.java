package com.wernertest.auth.service;

import com.wernertest.auth.entity.AuditLog;
import com.wernertest.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(String action, String performedBy, String targetUser,
                          String organizationName, String details, Boolean success, String errorMessage) {
        AuditLog auditLog = AuditLog.builder()
            .action(action)
            .performedBy(performedBy)
            .targetUser(targetUser)
            .organizationName(organizationName)
            .details(details)
            .success(success)
            .errorMessage(errorMessage)
            .build();

        auditLogRepository.save(auditLog);

        if (success) {
            log.info("AUDIT: {} performed {} on {} in org {} - {}",
                performedBy, action, targetUser, organizationName, details);
        } else {
            log.warn("AUDIT FAILURE: {} attempted {} on {} in org {} - {} - Error: {}",
                performedBy, action, targetUser, organizationName, details, errorMessage);
        }
    }

    public Page<AuditLog> getAuditLogsForOrganization(String organizationName, Pageable pageable) {
        return auditLogRepository.findByOrganizationNameOrderByTimestampDesc(organizationName, pageable);
    }
}
