package com.insidehealthgt.hms.audit

import com.insidehealthgt.hms.entity.AuditAction
import java.time.LocalDateTime

/**
 * Event published by AuditEntityListener during JPA lifecycle callbacks.
 * Processed asynchronously by AuditEventHandler after the transaction commits.
 */
data class AuditEvent(
    val userId: Long?,
    val username: String?,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long,
    val oldValues: String?,
    val newValues: String?,
    val changedFields: List<String>?,
    val ipAddress: String?,
    val timestamp: LocalDateTime,
)
