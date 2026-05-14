package com.insidehealthgt.hms.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.repository.AuditLogRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Daily job: detect inventory_items.quantity drift from SUM(active non-recalled lots).
 * Logs to audit_logs with status='FAILED' and details JSON containing the delta. No auto-heal.
 */
@Component
class InventoryQuantityDriftCheckJob(
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    fun checkDrift() {
        val items = itemRepository.findAllByLotTrackingEnabledTrue()
        var drifted = 0
        items.forEach { item ->
            val sum = lotRepository.sumQuantityOnHand(item.id!!)
            if (sum != item.quantity) {
                drifted++
                log.warn("Inventory drift detected: item={} quantity={} sumOfLots={}", item.id, item.quantity, sum)
                val details = objectMapper.writeValueAsString(
                    mapOf("reason" to "quantity_drift", "stored" to item.quantity, "sumOfLots" to sum),
                )
                auditLogRepository.save(
                    AuditLog(
                        userId = null,
                        username = "system",
                        action = AuditAction.UPDATE,
                        entityType = "InventoryItem",
                        entityId = item.id!!,
                        status = "FAILED",
                        details = details,
                        timestamp = LocalDateTime.now(),
                        createdAt = LocalDateTime.now(),
                    ),
                )
            }
        }
        log.info("Drift check finished. Items inspected: {}, drift rows: {}", items.size, drifted)
    }
}
