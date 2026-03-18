package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.SettleDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.UpdateDoctorFeeStatusRequest
import com.insidehealthgt.hms.dto.response.DoctorFeeResponse
import com.insidehealthgt.hms.dto.response.DoctorFeeSummaryResponse
import com.insidehealthgt.hms.entity.DoctorFee
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.DoctorFeeRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
@Service
class DoctorFeeService(
    private val doctorFeeRepository: DoctorFeeRepository,
    private val employeeService: TreasuryEmployeeService,
    private val patientChargeRepository: PatientChargeRepository,
    private val expenseService: ExpenseService,
    private val fileStorageService: FileStorageService,
) {

    companion object {
        private const val NET_AMOUNT_SCALE = 2
        private const val DOCTOR_FEE_REF_PREFIX = "DOCTOR-FEE-"
        private const val SUMMARY_IDX_TOTAL = 0
        private const val SUMMARY_IDX_GROSS = 1
        private const val SUMMARY_IDX_NET = 2
        private const val SUMMARY_IDX_PENDING = 3
        private const val SUMMARY_IDX_INVOICED = 4
        private const val SUMMARY_IDX_PAID = 5
        private val HUNDRED = BigDecimal(100)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun create(employeeId: Long, request: CreateDoctorFeeRequest): DoctorFeeResponse {
        val employee = employeeService.findEntityById(employeeId)
        if (employee.employeeType != EmployeeType.DOCTOR) {
            throw BadRequestException("Doctor fees are only applicable to DOCTOR employees")
        }

        if (request.patientChargeId != null) {
            if (!patientChargeRepository.existsById(request.patientChargeId)) {
                throw ResourceNotFoundException("Patient charge not found with id: ${request.patientChargeId}")
            }
        }

        val commissionPct = request.commissionPct ?: employee.hospitalCommissionPct
        val netAmount = computeNetAmount(request.grossAmount, commissionPct)

        val fee = DoctorFee(
            treasuryEmployee = employee,
            patientChargeId = request.patientChargeId,
            billingType = request.billingType,
            grossAmount = request.grossAmount,
            commissionPct = commissionPct,
            netAmount = netAmount,
            feeDate = request.feeDate,
            description = request.description?.takeIf { it.isNotBlank() },
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val saved = try {
            doctorFeeRepository.save(fee)
        } catch (@Suppress("SwallowedException") ex: DataIntegrityViolationException) {
            throw ConflictException(
                "A HOSPITAL_BILLED doctor fee already exists for charge ${request.patientChargeId}",
            )
        }
        return DoctorFeeResponse.from(saved)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun updateStatus(employeeId: Long, id: Long, request: UpdateDoctorFeeStatusRequest): DoctorFeeResponse {
        val fee = findEntityById(employeeId, id)
        if (request.status != DoctorFeeStatus.INVOICED) {
            throw BadRequestException("Only transition to INVOICED is allowed via this endpoint")
        }
        if (fee.status != DoctorFeeStatus.PENDING) {
            throw BadRequestException("Can only transition to INVOICED from PENDING status")
        }
        if (request.doctorInvoiceNumber.isNullOrBlank()) {
            throw BadRequestException("Doctor invoice number is required for INVOICED status")
        }
        fee.status = DoctorFeeStatus.INVOICED
        fee.doctorInvoiceNumber = request.doctorInvoiceNumber
        val saved = doctorFeeRepository.save(fee)
        return DoctorFeeResponse.from(saved)
    }

    @Transactional
    fun uploadInvoiceDocument(employeeId: Long, id: Long, file: MultipartFile): DoctorFeeResponse {
        val fee = findEntityById(employeeId, id)
        val storagePath = fileStorageService.storeDoctorFeeInvoice(fee.id!!, file)
        fee.invoiceDocumentPath = storagePath
        val saved = doctorFeeRepository.save(fee)
        return DoctorFeeResponse.from(saved)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun settle(employeeId: Long, id: Long, request: SettleDoctorFeeRequest): DoctorFeeResponse {
        val fee = doctorFeeRepository.findByIdForUpdate(id)
            .orElseThrow { ResourceNotFoundException("Doctor fee not found with id: $id") }
        if (fee.treasuryEmployee.id != employeeId) {
            throw ResourceNotFoundException("Doctor fee not found with id: $id")
        }
        if (fee.status != DoctorFeeStatus.INVOICED) {
            throw BadRequestException("Can only settle fees in INVOICED status")
        }
        if (fee.invoiceDocumentPath.isNullOrBlank()) {
            throw BadRequestException("Invoice document must be uploaded before settlement")
        }

        val reference = "$DOCTOR_FEE_REF_PREFIX${fee.id}"

        // Auto-create expense
        val savedExpense = expenseService.createPaidExpense(
            CreatePaidExpenseCommand(
                supplierName = fee.treasuryEmployee.fullName,
                category = ExpenseCategory.SERVICES,
                amount = fee.netAmount,
                expenseDate = request.paymentDate,
                invoiceNumber = fee.doctorInvoiceNumber ?: reference,
                bankAccountId = request.bankAccountId,
                paymentDate = request.paymentDate,
                paymentReference = reference,
                invoiceDocumentPath = fee.invoiceDocumentPath,
                treasuryEmployeeId = fee.treasuryEmployee.id,
                notes = request.notes,
            ),
        )

        // Mark fee as paid
        fee.status = DoctorFeeStatus.PAID
        fee.expenseId = savedExpense.id
        val saved = doctorFeeRepository.save(fee)
        return DoctorFeeResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun findAll(
        employeeId: Long,
        status: DoctorFeeStatus?,
        from: LocalDate?,
        to: LocalDate?,
    ): List<DoctorFeeResponse> {
        employeeService.findEntityById(employeeId) // verify exists
        val spec = buildSpecification(employeeId, status, from, to)
        return doctorFeeRepository.findAll(spec).map { DoctorFeeResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findById(employeeId: Long, id: Long): DoctorFeeResponse {
        val fee = findEntityById(employeeId, id)
        return DoctorFeeResponse.from(fee)
    }

    @Transactional
    fun delete(employeeId: Long, id: Long) {
        val fee = findEntityById(employeeId, id)
        if (fee.status != DoctorFeeStatus.PENDING) {
            throw BadRequestException("Can only delete fees in PENDING status")
        }
        fee.deletedAt = LocalDateTime.now()
        doctorFeeRepository.save(fee)
    }

    @Transactional(readOnly = true)
    fun getSummary(employeeId: Long): DoctorFeeSummaryResponse {
        val employee = employeeService.findEntityById(employeeId)
        val rows = doctorFeeRepository.aggregateSummary(employeeId)
        val row = rows.first()

        val totalFees = (row[SUMMARY_IDX_TOTAL] as Long).toInt()
        val totalGross = row[SUMMARY_IDX_GROSS] as BigDecimal
        val totalNet = row[SUMMARY_IDX_NET] as BigDecimal
        val pendingCount = (row[SUMMARY_IDX_PENDING] as Long).toInt()
        val invoicedCount = (row[SUMMARY_IDX_INVOICED] as Long).toInt()
        val paidCount = (row[SUMMARY_IDX_PAID] as Long).toInt()

        return DoctorFeeSummaryResponse(
            employeeId = employee.id!!,
            employeeName = employee.fullName,
            totalFees = totalFees,
            totalGross = totalGross,
            totalNet = totalNet,
            totalCommission = totalGross.subtract(totalNet),
            pendingCount = pendingCount,
            invoicedCount = invoicedCount,
            paidCount = paidCount,
        )
    }

    private fun findEntityById(employeeId: Long, id: Long): DoctorFee {
        val fee = doctorFeeRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Doctor fee not found with id: $id") }
        if (fee.treasuryEmployee.id != employeeId) {
            throw ResourceNotFoundException("Doctor fee not found with id: $id")
        }
        return fee
    }

    private fun computeNetAmount(grossAmount: BigDecimal, commissionPct: BigDecimal): BigDecimal {
        val commission = grossAmount.multiply(commissionPct).divide(HUNDRED, NET_AMOUNT_SCALE, RoundingMode.HALF_UP)
        return grossAmount.subtract(commission).setScale(NET_AMOUNT_SCALE, RoundingMode.HALF_UP)
    }

    private fun buildSpecification(
        employeeId: Long,
        status: DoctorFeeStatus?,
        from: LocalDate?,
        to: LocalDate?,
    ): Specification<DoctorFee> = Specification { root, _, cb ->
        val predicates = mutableListOf(
            cb.equal(root.get<Long>("treasuryEmployee").get<Long>("id"), employeeId),
        )
        status?.let { predicates.add(cb.equal(root.get<DoctorFeeStatus>("status"), it)) }
        from?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("feeDate"), it)) }
        to?.let { predicates.add(cb.lessThanOrEqualTo(root.get("feeDate"), it)) }
        cb.and(*predicates.toTypedArray())
    }
}
