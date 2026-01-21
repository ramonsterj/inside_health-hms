package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import java.time.LocalDateTime

data class AuditLogResponse(
    val id: Long,
    val userId: Long?,
    val username: String?,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long,
    val oldValues: String?,
    val newValues: String?,
    val ipAddress: String?,
    val timestamp: LocalDateTime,
) {
    companion object {
        fun from(auditLog: AuditLog): AuditLogResponse = AuditLogResponse(
            id = auditLog.id!!,
            userId = auditLog.userId,
            username = auditLog.username,
            action = auditLog.action,
            entityType = auditLog.entityType,
            entityId = auditLog.entityId,
            oldValues = auditLog.oldValues,
            newValues = auditLog.newValues,
            ipAddress = auditLog.ipAddress,
            timestamp = auditLog.timestamp,
        )
    }
}
