package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.InvoiceResponse
import com.insidehealthgt.hms.entity.Invoice
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InvoiceRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val INVOICE_NUMBER_PAD_LENGTH = 4

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val chargeRepository: PatientChargeRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun getInvoice(admissionId: Long): InvoiceResponse {
        val invoice = invoiceRepository.findByAdmissionId(admissionId)
            ?: throw ResourceNotFoundException("Invoice not found for admission: $admissionId")

        val charges = chargeRepository.findByInvoiceIdOrderByChargeDateAsc(invoice.id!!)

        val createdByUser = invoice.createdBy?.let { userRepository.findById(it).orElse(null) }
        return InvoiceResponse.from(invoice, charges, createdByUser)
    }

    @Transactional
    @Suppress("ThrowsCount")
    fun generateInvoice(admissionId: Long): InvoiceResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        if (!admission.isDischarged()) {
            throw BadRequestException("Invoice can only be generated for discharged admissions")
        }

        if (invoiceRepository.existsByAdmissionId(admissionId)) {
            throw ConflictException("Invoice already exists for admission: $admissionId")
        }

        val unbilledCharges = chargeRepository.findUnbilledByAdmissionId(admissionId)
        if (unbilledCharges.isEmpty()) {
            throw BadRequestException("No unbilled charges found for admission: $admissionId")
        }

        val totalAmount = unbilledCharges.sumOf { it.totalAmount }

        val invoice = Invoice(
            invoiceNumber = "PENDING",
            admission = admission,
            totalAmount = totalAmount,
            chargeCount = unbilledCharges.size,
        )

        val savedInvoice = invoiceRepository.save(invoice)

        // Generate invoice number from ID
        val year = LocalDate.now().year
        savedInvoice.invoiceNumber = "INV-$year-${savedInvoice.id.toString().padStart(INVOICE_NUMBER_PAD_LENGTH, '0')}"
        invoiceRepository.save(savedInvoice)

        // Link all unbilled charges to this invoice
        unbilledCharges.forEach { charge ->
            charge.invoice = savedInvoice
        }
        chargeRepository.saveAll(unbilledCharges)

        val createdByUser = savedInvoice.createdBy?.let { userRepository.findById(it).orElse(null) }
        return InvoiceResponse.from(savedInvoice, unbilledCharges, createdByUser)
    }
}
