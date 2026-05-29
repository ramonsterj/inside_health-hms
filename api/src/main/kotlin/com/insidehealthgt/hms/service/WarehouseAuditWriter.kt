package com.insidehealthgt.hms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.repository.AuditLogRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Writes warehouse transfer/charge audit rows in their own REQUIRES_NEW
 * transaction so an audit failure cannot roll back the underlying stock change
 * (matches the V099 admission-export pattern, NFR reliability / AC-17).
 */
@Component
class WarehouseAuditWriter(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun writeTransfer(userId: Long?, username: String?, transferId: Long, details: Map<String, Any?>) {
        save(userId, username, AuditAction.WAREHOUSE_TRANSFER, "InventoryTransfer", transferId, details)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun writeCharge(userId: Long?, username: String?, warehouseChargeId: Long, details: Map<String, Any?>) {
        save(userId, username, AuditAction.WAREHOUSE_CHARGE, "WarehouseCharge", warehouseChargeId, details)
    }

    @Suppress("LongParameterList")
    private fun save(
        userId: Long?,
        username: String?,
        action: AuditAction,
        entityType: String,
        entityId: Long,
        details: Map<String, Any?>,
    ) {
        val now = LocalDateTime.now()
        auditLogRepository.save(
            AuditLog(
                userId = userId,
                username = username,
                action = action,
                entityType = entityType,
                entityId = entityId,
                status = "SUCCESS",
                details = objectMapper.writeValueAsString(details),
                timestamp = now,
                createdAt = now,
            ),
        )
    }
}
