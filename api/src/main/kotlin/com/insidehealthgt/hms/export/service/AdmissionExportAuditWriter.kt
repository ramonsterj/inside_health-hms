package com.insidehealthgt.hms.export.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.repository.AuditLogRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Writes admission-export audit rows in their own REQUIRES_NEW transaction so the
 * audit row is durable regardless of the outer request's success or failure.
 */
@Component
class AdmissionExportAuditWriter(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun writeSuccess(
        userId: Long?,
        username: String?,
        admissionId: Long,
        ipAddress: String?,
        details: Map<String, Any?>,
    ): AuditLog = save(userId, username, admissionId, ipAddress, "SUCCESS", details)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun writeFailure(
        userId: Long?,
        username: String?,
        admissionId: Long,
        ipAddress: String?,
        details: Map<String, Any?>,
    ): AuditLog = save(userId, username, admissionId, ipAddress, "FAILED", details)

    private fun save(
        userId: Long?,
        username: String?,
        admissionId: Long,
        ipAddress: String?,
        status: String,
        details: Map<String, Any?>,
    ): AuditLog {
        val now = LocalDateTime.now()
        val auditLog = AuditLog(
            userId = userId,
            username = username,
            action = AuditAction.ADMISSION_EXPORT,
            entityType = "Admission",
            entityId = admissionId,
            ipAddress = ipAddress,
            status = status,
            details = objectMapper.writeValueAsString(details),
            timestamp = now,
            createdAt = now,
        )
        return auditLogRepository.save(auditLog)
    }
}
