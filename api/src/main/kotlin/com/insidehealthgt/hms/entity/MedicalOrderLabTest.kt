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
 * A line item on a `LABORATORIOS` [MedicalOrder]. References the chosen [LabProviderTest]
 * but **snapshots** its display name, cost, and sales price at creation time so later
 * catalog edits never retroactively change a recorded order or its already-created charge.
 */
@Entity
@Table(name = "medical_order_lab_tests")
@SQLRestriction("deleted_at IS NULL")
class MedicalOrderLabTest(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_order_id", nullable = false)
    var medicalOrder: MedicalOrder,

    @Column(name = "lab_provider_test_id", nullable = false)
    var labProviderTestId: Long,

    @Column(name = "lab_test_id", nullable = false)
    var labTestId: Long,

    @Column(name = "display_name", nullable = false, length = 200)
    var displayName: String,

    @Column(nullable = false, precision = 12, scale = 2)
    var cost: BigDecimal,

    @Column(name = "sales_price", nullable = false, precision = 12, scale = 2)
    var salesPrice: BigDecimal,

) : BaseEntity()
