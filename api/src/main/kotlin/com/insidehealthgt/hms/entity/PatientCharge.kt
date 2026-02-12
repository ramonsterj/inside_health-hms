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
import java.time.LocalDate

/**
 * Represents an immutable billing charge for a patient admission.
 *
 * Charges are append-only: no update endpoints exist. To correct a charge,
 * create a new charge with type [ChargeType.ADJUSTMENT], a negative [totalAmount],
 * and a mandatory [reason] explaining the correction.
 */
@Entity
@Table(name = "patient_charges")
@SQLRestriction("deleted_at IS NULL")
class PatientCharge(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(name = "charge_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var chargeType: ChargeType,

    @Column(nullable = false, length = 500)
    var description: String,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    var unitPrice: BigDecimal,

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal,

    @Column(name = "charge_date", nullable = false)
    var chargeDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    var inventoryItem: InventoryItem? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    var room: Room? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    var invoice: Invoice? = null,

    @Column(length = 500)
    var reason: String? = null,

) : BaseEntity()
