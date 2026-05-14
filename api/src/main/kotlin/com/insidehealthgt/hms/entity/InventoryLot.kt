package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate

@Entity
@Table(name = "inventory_lots")
@SQLRestriction("deleted_at IS NULL")
class InventoryLot(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: InventoryItem,

    @Column(name = "lot_number", length = 50)
    var lotNumber: String? = null,

    @Column(name = "expiration_date", nullable = false)
    var expirationDate: LocalDate,

    @Column(name = "quantity_on_hand", nullable = false)
    var quantityOnHand: Int,

    @Column(name = "received_at", nullable = false)
    var receivedAt: LocalDate,

    @Column(length = 150)
    var supplier: String? = null,

    @Column(length = 500)
    var notes: String? = null,

    @Column(nullable = false)
    var recalled: Boolean = false,

    @Column(name = "recalled_reason", length = 500)
    var recalledReason: String? = null,

    @Column(name = "synthetic_legacy", nullable = false)
    var syntheticLegacy: Boolean = false,

) : BaseEntity()
