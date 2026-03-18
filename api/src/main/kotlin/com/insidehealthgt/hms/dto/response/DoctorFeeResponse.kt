package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.DoctorFee
import com.insidehealthgt.hms.entity.DoctorFeeBillingType
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DoctorFeeResponse(
    val id: Long,
    val treasuryEmployeeId: Long,
    val employeeName: String,
    val patientChargeId: Long?,
    val billingType: DoctorFeeBillingType,
    val grossAmount: BigDecimal,
    val commissionPct: BigDecimal,
    val netAmount: BigDecimal,
    val status: DoctorFeeStatus,
    val doctorInvoiceNumber: String?,
    val invoiceDocumentPath: String?,
    val expenseId: Long?,
    val feeDate: LocalDate,
    val description: String?,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(fee: DoctorFee): DoctorFeeResponse = DoctorFeeResponse(
            id = fee.id!!,
            treasuryEmployeeId = fee.treasuryEmployee.id!!,
            employeeName = fee.treasuryEmployee.fullName,
            patientChargeId = fee.patientChargeId,
            billingType = fee.billingType,
            grossAmount = fee.grossAmount,
            commissionPct = fee.commissionPct,
            netAmount = fee.netAmount,
            status = fee.status,
            doctorInvoiceNumber = fee.doctorInvoiceNumber,
            invoiceDocumentPath = fee.invoiceDocumentPath,
            expenseId = fee.expenseId,
            feeDate = fee.feeDate,
            description = fee.description,
            notes = fee.notes,
            createdAt = fee.createdAt,
            updatedAt = fee.updatedAt,
        )
    }
}

data class DoctorFeeSummaryResponse(
    val employeeId: Long,
    val employeeName: String,
    val totalFees: Int,
    val totalGross: BigDecimal,
    val totalNet: BigDecimal,
    val totalCommission: BigDecimal,
    val pendingCount: Int,
    val invoicedCount: Int,
    val paidCount: Int,
)
