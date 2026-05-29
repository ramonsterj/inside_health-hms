package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

/**
 * Per-(item, warehouse, optional lot) on-hand quantity. The lot is nullable: one
 * row per (item, warehouse) for non-lot-tracked items; one row per
 * (item, warehouse, lot) for lot-tracked items. Replaces the legacy global
 * `inventory_items.quantity` / `inventory_lots.quantity_on_hand` columns.
 *
 * `quantity` is NUMERIC(14,3) per the spec DDL; the public catalog/movement
 * surface remains integer, so callers sum and `.toInt()` when surfacing.
 */
@Entity
@Table(name = "inventory_warehouse_stock")
@SQLRestriction("deleted_at IS NULL")
class InventoryWarehouseStock(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: InventoryItem,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    var lot: InventoryLot? = null,

    @Column(nullable = false, precision = 14, scale = 3)
    var quantity: BigDecimal = BigDecimal.ZERO,

) : BaseEntity()
