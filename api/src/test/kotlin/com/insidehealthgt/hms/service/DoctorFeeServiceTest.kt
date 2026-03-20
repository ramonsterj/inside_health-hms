package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.SettleDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.UpdateDoctorFeeStatusRequest
import com.insidehealthgt.hms.entity.DoctorFee
import com.insidehealthgt.hms.entity.DoctorFeeBillingType
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.DoctorFeeRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.domain.Specification
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("TooManyFunctions", "LargeClass")
class DoctorFeeServiceTest {

    private lateinit var doctorFeeRepository: DoctorFeeRepository
    private lateinit var employeeService: TreasuryEmployeeService
    private lateinit var patientChargeRepository: PatientChargeRepository
    private lateinit var expenseService: ExpenseService
    private lateinit var fileStorageService: FileStorageService
    private lateinit var service: DoctorFeeService

    private fun makeEmployee(
        id: Long = 10L,
        type: EmployeeType = EmployeeType.DOCTOR,
        commissionPct: BigDecimal = BigDecimal("15.00"),
    ): TreasuryEmployee {
        val emp = TreasuryEmployee(
            fullName = "Dr. Test",
            employeeType = type,
            hospitalCommissionPct = commissionPct,
            hireDate = LocalDate.of(2020, 1, 1),
        )
        emp.id = id
        return emp
    }

    private fun makeDoctorFee(
        id: Long = 1L,
        employee: TreasuryEmployee = makeEmployee(),
        billingType: DoctorFeeBillingType = DoctorFeeBillingType.HOSPITAL_BILLED,
        grossAmount: BigDecimal = BigDecimal("1000.00"),
        commissionPct: BigDecimal = BigDecimal("15.00"),
        netAmount: BigDecimal = BigDecimal("850.00"),
        status: DoctorFeeStatus = DoctorFeeStatus.PENDING,
        invoiceDocumentPath: String? = null,
        doctorInvoiceNumber: String? = null,
        expenseId: Long? = null,
    ): DoctorFee {
        val fee = DoctorFee(
            treasuryEmployee = employee,
            billingType = billingType,
            grossAmount = grossAmount,
            commissionPct = commissionPct,
            netAmount = netAmount,
            status = status,
            invoiceDocumentPath = invoiceDocumentPath,
            doctorInvoiceNumber = doctorInvoiceNumber,
            expenseId = expenseId,
            feeDate = LocalDate.now(),
        )
        fee.id = id
        return fee
    }

    private fun makeExpense(id: Long = 100L): Expense {
        val expense = Expense(
            supplierName = "Dr. Test",
            category = ExpenseCategory.SERVICES,
            amount = BigDecimal("850.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "DOCTOR-FEE-1",
            status = ExpenseStatus.PAID,
            paidAmount = BigDecimal("850.00"),
        )
        expense.id = id
        return expense
    }

    @BeforeEach
    fun setUp() {
        doctorFeeRepository = mock()
        employeeService = mock()
        patientChargeRepository = mock()
        expenseService = mock()
        fileStorageService = mock()

        service = DoctorFeeService(
            doctorFeeRepository,
            employeeService,
            patientChargeRepository,
            expenseService,
            fileStorageService,
        )
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    fun `create returns DoctorFeeResponse for valid request`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)
        val fee = makeDoctorFee(employee = employee)
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenReturn(fee)

        val request = CreateDoctorFeeRequest(
            billingType = DoctorFeeBillingType.HOSPITAL_BILLED,
            grossAmount = BigDecimal("1000.00"),
            commissionPct = null,
            feeDate = LocalDate.now(),
        )

        val result = service.create(10L, request)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(10L, result.treasuryEmployeeId)
        assertEquals(BigDecimal("15.00"), result.commissionPct)
        verify(doctorFeeRepository).save(any())
    }

    @Test
    fun `create uses provided commissionPct when specified`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)
        val fee = makeDoctorFee(
            employee = employee,
            commissionPct = BigDecimal("20.00"),
            netAmount = BigDecimal("800.00"),
        )
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenReturn(fee)

        val request = CreateDoctorFeeRequest(
            billingType = DoctorFeeBillingType.EXTERNAL,
            grossAmount = BigDecimal("1000.00"),
            commissionPct = BigDecimal("20.00"),
            feeDate = LocalDate.now(),
        )

        val result = service.create(10L, request)

        assertNotNull(result)
        assertEquals(BigDecimal("20.00"), result.commissionPct)
    }

    @Test
    fun `create throws BadRequestException when employee is not DOCTOR`() {
        val payrollEmployee = makeEmployee(type = EmployeeType.PAYROLL)
        whenever(employeeService.findEntityById(10L)).thenReturn(payrollEmployee)

        val request = CreateDoctorFeeRequest(
            billingType = DoctorFeeBillingType.HOSPITAL_BILLED,
            grossAmount = BigDecimal("1000.00"),
            feeDate = LocalDate.now(),
        )

        assertThrows<BadRequestException> { service.create(10L, request) }
    }

    @Test
    fun `create throws ResourceNotFoundException when patient charge does not exist`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)
        whenever(patientChargeRepository.existsById(99L)).thenReturn(false)

        val request = CreateDoctorFeeRequest(
            patientChargeId = 99L,
            billingType = DoctorFeeBillingType.HOSPITAL_BILLED,
            grossAmount = BigDecimal("1000.00"),
            feeDate = LocalDate.now(),
        )

        assertThrows<ResourceNotFoundException> { service.create(10L, request) }
    }

    @Test
    fun `create throws ConflictException on duplicate HOSPITAL_BILLED charge`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)
        whenever(patientChargeRepository.existsById(5L)).thenReturn(true)
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenThrow(DataIntegrityViolationException("duplicate"))

        val request = CreateDoctorFeeRequest(
            patientChargeId = 5L,
            billingType = DoctorFeeBillingType.HOSPITAL_BILLED,
            grossAmount = BigDecimal("1000.00"),
            feeDate = LocalDate.now(),
        )

        assertThrows<ConflictException> { service.create(10L, request) }
    }

    // ─── updateStatus ───────────────────────────────────────────────────────────

    @Test
    fun `updateStatus throws BadRequestException when invoice document is missing`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.PENDING, invoiceDocumentPath = null)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        val request = UpdateDoctorFeeStatusRequest(
            status = DoctorFeeStatus.INVOICED,
            doctorInvoiceNumber = "INV-2026-001",
        )

        assertThrows<BadRequestException> { service.updateStatus(10L, 1L, request) }
    }

    @Test
    fun `updateStatus transitions from PENDING to INVOICED`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(
            employee = employee,
            status = DoctorFeeStatus.PENDING,
            invoiceDocumentPath = "/storage/doctor-fees/1/invoice.pdf",
        )
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenAnswer { it.arguments[0] }

        val request = UpdateDoctorFeeStatusRequest(
            status = DoctorFeeStatus.INVOICED,
            doctorInvoiceNumber = "INV-2026-001",
        )

        val result = service.updateStatus(10L, 1L, request)

        assertNotNull(result)
        assertEquals(DoctorFeeStatus.INVOICED, result.status)
        assertEquals("INV-2026-001", result.doctorInvoiceNumber)
    }

    @Test
    fun `updateStatus throws BadRequestException for non-INVOICED target status`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.PENDING)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        val request = UpdateDoctorFeeStatusRequest(
            status = DoctorFeeStatus.PAID,
            doctorInvoiceNumber = "INV-2026-001",
        )

        assertThrows<BadRequestException> { service.updateStatus(10L, 1L, request) }
    }

    @Test
    fun `updateStatus throws BadRequestException when current status is not PENDING`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.INVOICED)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        val request = UpdateDoctorFeeStatusRequest(
            status = DoctorFeeStatus.INVOICED,
            doctorInvoiceNumber = "INV-2026-001",
        )

        assertThrows<BadRequestException> { service.updateStatus(10L, 1L, request) }
    }

    @Test
    fun `updateStatus throws BadRequestException when invoice number is blank`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.PENDING)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        val request = UpdateDoctorFeeStatusRequest(
            status = DoctorFeeStatus.INVOICED,
            doctorInvoiceNumber = "   ",
        )

        assertThrows<BadRequestException> { service.updateStatus(10L, 1L, request) }
    }

    // ─── uploadInvoiceDocument ──────────────────────────────────────────────────

    @Test
    fun `uploadInvoiceDocument stores file and updates path`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))
        whenever(fileStorageService.storeDoctorFeeInvoice(any(), any()))
            .thenReturn("/storage/doctor-fees/1/invoice.pdf")
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenAnswer { it.arguments[0] }

        val mockFile = MockMultipartFile("invoice", "invoice.pdf", "application/pdf", ByteArray(10))

        val result = service.uploadInvoiceDocument(10L, 1L, mockFile)

        assertNotNull(result)
        assertEquals("/storage/doctor-fees/1/invoice.pdf", result.invoiceDocumentPath)
        verify(fileStorageService).storeDoctorFeeInvoice(1L, mockFile)
    }

    // ─── settle ─────────────────────────────────────────────────────────────────

    @Test
    fun `settle creates expense and marks fee as PAID`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(
            employee = employee,
            status = DoctorFeeStatus.INVOICED,
            invoiceDocumentPath = "/storage/doctor-fees/1/invoice.pdf",
            doctorInvoiceNumber = "INV-2026-001",
        )
        whenever(doctorFeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fee))
        val savedExpense = makeExpense()
        whenever(expenseService.createPaidExpense(any())).thenReturn(savedExpense)
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenAnswer { it.arguments[0] }

        val request = SettleDoctorFeeRequest(
            bankAccountId = 20L,
            paymentDate = LocalDate.now(),
        )

        val result = service.settle(10L, 1L, request)

        assertNotNull(result)
        assertEquals(DoctorFeeStatus.PAID, result.status)
        assertEquals(100L, result.expenseId)
        verify(expenseService).createPaidExpense(any())
    }

    @Test
    fun `settle throws BadRequestException when fee is not INVOICED`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(
            employee = employee,
            status = DoctorFeeStatus.PENDING,
            invoiceDocumentPath = "/storage/doctor-fees/1/invoice.pdf",
        )
        whenever(doctorFeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fee))

        val request = SettleDoctorFeeRequest(
            bankAccountId = 20L,
            paymentDate = LocalDate.now(),
        )

        assertThrows<BadRequestException> { service.settle(10L, 1L, request) }
    }

    @Test
    fun `settle throws BadRequestException when invoice document is missing`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(
            employee = employee,
            status = DoctorFeeStatus.INVOICED,
            invoiceDocumentPath = null,
        )
        whenever(doctorFeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fee))

        val request = SettleDoctorFeeRequest(
            bankAccountId = 20L,
            paymentDate = LocalDate.now(),
        )

        assertThrows<BadRequestException> { service.settle(10L, 1L, request) }
    }

    @Test
    fun `settle throws ResourceNotFoundException when fee not found`() {
        whenever(doctorFeeRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty())

        val request = SettleDoctorFeeRequest(
            bankAccountId = 20L,
            paymentDate = LocalDate.now(),
        )

        assertThrows<ResourceNotFoundException> { service.settle(10L, 99L, request) }
    }

    // ─── findAll ────────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns list of fees for employee`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)
        val fee = makeDoctorFee(employee = employee)
        whenever(doctorFeeRepository.findAll(any<Specification<DoctorFee>>())).thenReturn(listOf(fee))

        val result = service.findAll(10L, null, null, null)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    // ─── findById ───────────────────────────────────────────────────────────────

    @Test
    fun `findById returns fee when found`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        val result = service.findById(10L, 1L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(10L, result.treasuryEmployeeId)
    }

    @Test
    fun `findById throws ResourceNotFoundException when employee mismatch`() {
        val employee = makeEmployee(id = 10L)
        val fee = makeDoctorFee(employee = employee)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        // Request with a different employeeId (20L) — fee belongs to employee 10L
        assertThrows<ResourceNotFoundException> { service.findById(20L, 1L) }
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Test
    fun `delete soft deletes PENDING fee`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.PENDING)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))
        whenever(doctorFeeRepository.save(any<DoctorFee>())).thenAnswer { it.arguments[0] }

        service.delete(10L, 1L)

        assertNotNull(fee.deletedAt)
        verify(doctorFeeRepository).save(fee)
    }

    @Test
    fun `delete throws BadRequestException when fee is not PENDING`() {
        val employee = makeEmployee()
        val fee = makeDoctorFee(employee = employee, status = DoctorFeeStatus.INVOICED)
        whenever(doctorFeeRepository.findById(1L)).thenReturn(Optional.of(fee))

        assertThrows<BadRequestException> { service.delete(10L, 1L) }
    }

    // ─── getSummary ─────────────────────────────────────────────────────────────

    @Test
    fun `getSummary returns aggregated summary`() {
        val employee = makeEmployee()
        whenever(employeeService.findEntityById(10L)).thenReturn(employee)

        val summaryRow: List<Array<Any>> = listOf(
            arrayOf(
                5L,
                BigDecimal("5000.00"),
                BigDecimal("4250.00"),
                2L,
                2L,
                1L,
                BigDecimal("850.00"),
            ),
        )
        whenever(doctorFeeRepository.aggregateSummary(10L)).thenReturn(summaryRow)

        val result = service.getSummary(10L)

        assertEquals(10L, result.employeeId)
        assertEquals("Dr. Test", result.employeeName)
        assertEquals(5, result.totalFees)
        assertEquals(BigDecimal("5000.00"), result.totalGross)
        assertEquals(BigDecimal("4250.00"), result.totalNet)
        assertEquals(BigDecimal("750.00"), result.totalCommission)
        assertEquals(2, result.pendingCount)
        assertEquals(2, result.invoicedCount)
        assertEquals(1, result.paidCount)
        assertEquals(BigDecimal("850.00"), result.amountPaid)
        assertEquals(BigDecimal("3400.00"), result.outstandingBalance)
    }
}
