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
 * Audit row for a non-medical consumable charged from a warehouse to an
 * admission (the customer's "load a broken/stained towel to room 203" flow).
 * Links to the resulting [PatientCharge] once billing creates it.
 */
@Entity
@Table(name = "warehouse_charges")
@SQLRestriction("deleted_at IS NULL")
class WarehouseCharge(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: InventoryItem,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    var lot: InventoryLot? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(nullable = false, precision = 14, scale = 3)
    var quantity: BigDecimal,

    @Column(nullable = false, precision = 14, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false, length = 500)
    var reason: String,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id")
    var charge: PatientCharge? = null,

) : BaseEntity()
