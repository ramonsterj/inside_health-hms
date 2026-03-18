package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Invoice
import java.math.BigDecimal

data class InvoiceSummaryResponse(
    val id: Long,
    val invoiceNumber: String,
    val patientName: String,
    val totalAmount: BigDecimal,
) {
    companion object {
        fun from(invoice: Invoice): InvoiceSummaryResponse {
            val patient = invoice.admission.patient
            return InvoiceSummaryResponse(
                id = invoice.id!!,
                invoiceNumber = invoice.invoiceNumber,
                patientName = "${patient.firstName} ${patient.lastName}",
                totalAmount = invoice.totalAmount,
            )
        }
    }
}
