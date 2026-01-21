package com.insidehealthgt.hms.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime

/**
 * Handles AuditEvent published by AuditEntityListener.
 * Uses @TransactionalEventListener to process events AFTER the main transaction commits,
 * which avoids Hibernate 6's restrictions on persistence operations during flush callbacks.
 */
@Component
class AuditEventHandler(private val auditLogRepository: AuditLogRepository, private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(AuditEventHandler::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleAuditEvent(event: AuditEvent) {
        log.info("Received audit event: {} {} {}", event.action, event.entityType, event.entityId)
        try {
            val changedFieldsJson = event.changedFields?.let { objectMapper.writeValueAsString(it) }

            val auditLog = AuditLog(
                userId = event.userId,
                username = event.username,
                action = event.action,
                entityType = event.entityType,
                entityId = event.entityId,
                oldValues = event.oldValues,
                newValues = event.newValues,
                changedFields = changedFieldsJson,
                ipAddress = event.ipAddress,
                timestamp = event.timestamp,
                createdAt = LocalDateTime.now(),
            )

            auditLogRepository.save(auditLog)
            log.info(
                "Audit log saved: {} {} {} (id={})",
                event.action,
                event.entityType,
                event.entityId,
                auditLog.id,
            )
        } catch (e: DataAccessException) {
            log.error(
                "Failed to save audit log for {} {} {}",
                event.action,
                event.entityType,
                event.entityId,
                e,
            )
        }
    }
}
