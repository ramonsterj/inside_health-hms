package com.insidehealthgt.hms.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.repository.AuditLogRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Daily integrity job. The global `inventory_items.quantity` denormalization is
 * gone (stock lives per-warehouse in `inventory_warehouse_stock`), so there is no
 * scalar drift to reconcile. Instead this flags an ops concern: warehouse stock
 * still sitting on **recalled** lots — those rows should be quarantined / written
 * off. Logged to audit_logs with status='FAILED'; no auto-heal.
 */
@Component
class RecalledLotStockCheckJob(
    private val stockRepository: InventoryWarehouseStockRepository,
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    fun checkRecalledLotStock() {
        val recalledStock = stockRepository.findRecalledLotStock()
        recalledStock.forEach { row ->
            log.warn(
                "Recalled lot still holds stock: item={} lot={} warehouse={} quantity={}",
                row.itemId,
                row.lotId,
                row.warehouseId,
                row.quantity,
            )
            val details = objectMapper.writeValueAsString(
                mapOf(
                    "reason" to "recalled_lot_stock",
                    "lotId" to row.lotId,
                    "warehouseId" to row.warehouseId,
                    "quantity" to row.quantity,
                ),
            )
            auditLogRepository.save(
                AuditLog(
                    userId = null,
                    username = "system",
                    action = AuditAction.UPDATE,
                    entityType = "InventoryWarehouseStock",
                    entityId = row.itemId,
                    status = "FAILED",
                    details = details,
                    timestamp = LocalDateTime.now(),
                    createdAt = LocalDateTime.now(),
                ),
            )
        }
        log.info("Recalled-lot stock check finished. Flagged rows: {}", recalledStock.size)
    }
}
