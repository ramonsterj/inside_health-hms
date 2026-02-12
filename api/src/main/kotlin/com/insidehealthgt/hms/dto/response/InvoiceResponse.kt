package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.Invoice
import com.insidehealthgt.hms.entity.PatientCharge
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InvoiceResponse(
    val id: Long,
    val invoiceNumber: String,
    val admissionId: Long,
    val patientName: String,
    val admissionDate: LocalDate,
    val dischargeDate: LocalDate?,
    val totalAmount: BigDecimal,
    val chargeCount: Int,
    val chargeSummary: List<ChargeSummaryItem>,
    val generatedAt: LocalDateTime?,
    val generatedByName: String?,
) {
    companion object {
        fun from(invoice: Invoice, charges: List<PatientCharge>, createdByUser: User? = null): InvoiceResponse {
            val summary = charges.groupBy { it.chargeType }
                .map { (type, typeCharges) ->
                    ChargeSummaryItem(
                        chargeType = type,
                        count = typeCharges.size,
                        subtotal = typeCharges.sumOf { it.totalAmount },
                    )
                }
                .sortedBy { it.chargeType.name }

            val admission = invoice.admission
            val patient = admission.patient
            val patientName = "${patient.firstName} ${patient.lastName}"

            return InvoiceResponse(
                id = invoice.id!!,
                invoiceNumber = invoice.invoiceNumber,
                admissionId = admission.id!!,
                patientName = patientName,
                admissionDate = admission.admissionDate.toLocalDate(),
                dischargeDate = admission.dischargeDate?.toLocalDate(),
                totalAmount = invoice.totalAmount,
                chargeCount = invoice.chargeCount,
                chargeSummary = summary,
                generatedAt = invoice.createdAt,
                generatedByName = createdByUser?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() },
            )
        }
    }
}

data class ChargeSummaryItem(val chargeType: ChargeType, val count: Int, val subtotal: BigDecimal)
