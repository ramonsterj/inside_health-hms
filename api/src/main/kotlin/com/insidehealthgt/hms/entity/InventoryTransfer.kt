package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Atomic inter-warehouse transfer aggregate. Modeling it as its own row (rather
 * than just two movements) gives atomicity in the audit trail, a home for the
 * Phase 2 approval flow, and cleaner financial reporting. See
 * [com.insidehealthgt.hms.service.WarehouseTransferService].
 */
@Entity
@Table(name = "inventory_transfers")
@SQLRestriction("deleted_at IS NULL")
class InventoryTransfer(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    var sourceWarehouse: Warehouse,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id", nullable = false)
    var destinationWarehouse: Warehouse,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: InventoryItem,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    var lot: InventoryLot? = null,

    @Column(nullable = false, precision = 14, scale = 3)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: TransferStatus = TransferStatus.COMPLETED,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "issued_by", nullable = false)
    var issuedBy: Long,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(name = "completed_by")
    var completedBy: Long? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "cancelled_by")
    var cancelledBy: Long? = null,

) : BaseEntity()
