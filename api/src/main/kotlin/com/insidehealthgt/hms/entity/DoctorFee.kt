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

@Entity
@Table(name = "doctor_fees")
@SQLRestriction("deleted_at IS NULL")
class DoctorFee(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_employee_id", nullable = false)
    var treasuryEmployee: TreasuryEmployee,

    @Column(name = "patient_charge_id")
    var patientChargeId: Long? = null,

    @Column(name = "billing_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var billingType: DoctorFeeBillingType,

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    var grossAmount: BigDecimal,

    @Column(name = "commission_pct", nullable = false, precision = 5, scale = 2)
    var commissionPct: BigDecimal,

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    var netAmount: BigDecimal,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: DoctorFeeStatus = DoctorFeeStatus.PENDING,

    @Column(name = "doctor_invoice_number", length = 100)
    var doctorInvoiceNumber: String? = null,

    @Column(name = "invoice_document_path", length = 500)
    var invoiceDocumentPath: String? = null,

    @Column(name = "expense_id")
    var expenseId: Long? = null,

    @Column(name = "fee_date", nullable = false)
    var feeDate: LocalDate,

    @Column(length = 500)
    var description: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
