package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

@Entity
@Table(name = "invoices")
@SQLRestriction("deleted_at IS NULL")
class Invoice(

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    var invoiceNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "charge_count", nullable = false)
    var chargeCount: Int = 0,

    @Column(length = 1000)
    var notes: String? = null,

) : BaseEntity()
