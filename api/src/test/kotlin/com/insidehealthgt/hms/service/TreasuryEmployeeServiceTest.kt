package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.request.GeneratePayrollScheduleRequest
import com.insidehealthgt.hms.dto.request.RecordContractorPaymentRequest
import com.insidehealthgt.hms.dto.request.RecordPayrollPaymentRequest
import com.insidehealthgt.hms.dto.request.TerminateEmployeeRequest
import com.insidehealthgt.hms.dto.request.UpdateSalaryRequest
import com.insidehealthgt.hms.dto.request.UpdateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.response.EmployeePaymentType
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.DoctorFeeArrangement
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollPeriod
import com.insidehealthgt.hms.entity.PayrollStatus
import com.insidehealthgt.hms.entity.SalaryHistory
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.PayrollEntryRepository
import com.insidehealthgt.hms.repository.SalaryHistoryRepository
import com.insidehealthgt.hms.repository.TreasuryEmployeeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
class TreasuryEmployeeServiceTest {

    private lateinit var employeeRepository: TreasuryEmployeeRepository
    private lateinit var salaryHistoryRepository: SalaryHistoryRepository
    private lateinit var payrollEntryRepository: PayrollEntryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var expensePaymentRepository: ExpensePaymentRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var userRepository: UserRepository
    private lateinit var service: TreasuryEmployeeService

    private fun makeEmployee(
        type: EmployeeType = EmployeeType.PAYROLL,
        baseSalary: BigDecimal? = BigDecimal("5000.00"),
        contractedRate: BigDecimal? = null,
        active: Boolean = true,
        hireDate: LocalDate? = LocalDate.of(2024, 1, 1),
        id: Long = 1L,
    ): TreasuryEmployee {
        val e = TreasuryEmployee(
            fullName = "Ana Lopez",
            employeeType = type,
            baseSalary = baseSalary,
            contractedRate = contractedRate,
            hireDate = hireDate,
            active = active,
        )
        e.id = id
        return e
    }

    private fun makePayrollEntry(
        status: PayrollStatus = PayrollStatus.PENDING,
        dueDate: LocalDate = LocalDate.of(2026, 3, 31),
        id: Long = 10L,
        employee: TreasuryEmployee = makeEmployee(),
    ): PayrollEntry {
        val entry = PayrollEntry(
            employee = employee,
            year = 2026,
            period = PayrollPeriod.MARCH,
            periodLabel = "March 2026",
            baseSalary = BigDecimal("5000.00"),
            grossAmount = BigDecimal("5000.00"),
            dueDate = dueDate,
            status = status,
        )
        entry.id = id
        return entry
    }

    private fun makeBankAccount(id: Long = 20L): BankAccount {
        val account = BankAccount(
            name = "Banco Industrial",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal("10000.00"),
        )
        account.id = id
        return account
    }

    @BeforeEach
    fun setUp() {
        employeeRepository = mock()
        salaryHistoryRepository = mock()
        payrollEntryRepository = mock()
        expenseRepository = mock()
        expensePaymentRepository = mock()
        bankAccountService = mock()
        userRepository = mock()

        service = TreasuryEmployeeService(
            employeeRepository,
            salaryHistoryRepository,
            payrollEntryRepository,
            expenseRepository,
            expensePaymentRepository,
            bankAccountService,
            userRepository,
        )

        whenever(userRepository.findAllById(any())).thenReturn(emptyList())
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create throws BadRequestException when PAYROLL employee has no baseSalary`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Juan Perez",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = null,
        )

        assertThrows<BadRequestException> { service.create(request) }
        verify(employeeRepository, never()).save(any())
    }

    @Test
    fun `create throws BadRequestException when CONTRACTOR has no contractedRate`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Maria Contractor",
            employeeType = EmployeeType.CONTRACTOR,
            contractedRate = null,
        )

        assertThrows<BadRequestException> { service.create(request) }
        verify(employeeRepository, never()).save(any())
    }

    @Test
    fun `create PAYROLL employee also creates initial salary history`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Carlos Empleado",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("4500.00"),
            hireDate = LocalDate.of(2026, 1, 1),
        )
        val saved = makeEmployee()
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(saved)
        whenever(salaryHistoryRepository.save(any<SalaryHistory>())).thenAnswer { it.arguments[0] }

        service.create(request)

        verify(salaryHistoryRepository).save(any())
    }

    @Test
    fun `create CONTRACTOR does not create salary history`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Pedro Contractor",
            employeeType = EmployeeType.CONTRACTOR,
            contractedRate = BigDecimal("1000.00"),
        )
        val saved = makeEmployee(
            type = EmployeeType.CONTRACTOR,
            baseSalary = null,
            contractedRate = BigDecimal("1000.00"),
        )
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(saved)

        service.create(request)

        verify(salaryHistoryRepository, never()).save(any())
    }

    // ─── updateSalary ──────────────────────────────────────────────────────────

    @Test
    fun `updateSalary throws BadRequestException for non-PAYROLL employee`() {
        val contractor = makeEmployee(type = EmployeeType.CONTRACTOR)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))

        assertThrows<BadRequestException> {
            service.updateSalary(
                1L,
                UpdateSalaryRequest(newSalary = BigDecimal("3000.00"), effectiveFrom = LocalDate.now()),
            )
        }
    }

    @Test
    fun `updateSalary throws BadRequestException when effectiveFrom is not after current history start`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val currentHistory = SalaryHistory(
            employee = employee,
            baseSalary = BigDecimal("5000.00"),
            effectiveFrom = LocalDate.of(2026, 1, 1),
        )
        whenever(salaryHistoryRepository.findByEmployeeIdAndEffectiveToIsNull(1L)).thenReturn(currentHistory)

        assertThrows<BadRequestException> {
            service.updateSalary(
                1L,
                UpdateSalaryRequest(newSalary = BigDecimal("6000.00"), effectiveFrom = LocalDate.of(2026, 1, 1)),
            )
        }
    }

    @Test
    fun `updateSalary closes current history and creates new one and updates pending entries`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(employee)

        val currentHistory = SalaryHistory(
            employee = employee,
            baseSalary = BigDecimal("5000.00"),
            effectiveFrom = LocalDate.of(2026, 1, 1),
        )
        whenever(salaryHistoryRepository.findByEmployeeIdAndEffectiveToIsNull(1L)).thenReturn(currentHistory)
        whenever(salaryHistoryRepository.save(any<SalaryHistory>())).thenAnswer { it.arguments[0] }

        val pendingEntry = makePayrollEntry(dueDate = LocalDate.of(2026, 4, 30))
        whenever(payrollEntryRepository.findAllByEmployeeIdAndStatusOrderByDueDateAsc(1L, PayrollStatus.PENDING))
            .thenReturn(listOf(pendingEntry))
        whenever(payrollEntryRepository.saveAll(any<List<PayrollEntry>>())).thenAnswer { it.arguments[0] }

        service.updateSalary(
            1L,
            UpdateSalaryRequest(newSalary = BigDecimal("6000.00"), effectiveFrom = LocalDate.of(2026, 3, 1)),
        )

        assertNotNull(currentHistory.effectiveTo) // history was closed
        assertEquals(BigDecimal("6000.00"), pendingEntry.baseSalary) // pending entry updated
        verify(salaryHistoryRepository).save(currentHistory) // close old
        verify(salaryHistoryRepository).save(
            org.mockito.kotlin.argThat<SalaryHistory> { baseSalary == BigDecimal("6000.00") },
        ) // new history
    }

    // ─── terminate ─────────────────────────────────────────────────────────────

    @Test
    fun `terminate throws BadRequestException when employee is already inactive`() {
        val inactive = makeEmployee(active = false)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(inactive))

        assertThrows<BadRequestException> {
            service.terminate(1L, TerminateEmployeeRequest(terminationDate = LocalDate.now()))
        }
    }

    @Test
    fun `terminate sets active to false and terminationDate`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(employee)
        whenever(payrollEntryRepository.findAllByEmployeeIdAndStatusOrderByDueDateAsc(1L, PayrollStatus.PENDING))
            .thenReturn(emptyList())

        val terminationDate = LocalDate.of(2026, 3, 31)
        service.terminate(
            1L,
            TerminateEmployeeRequest(terminationDate = terminationDate, terminationReason = "Resigned"),
        )

        assertFalse(employee.active)
        assertEquals(terminationDate, employee.terminationDate)
        assertEquals("Resigned", employee.terminationReason)
    }

    @Test
    fun `terminate cancels pending payroll when cancelPendingPayroll is true`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(employee)

        val pendingEntry = makePayrollEntry()
        whenever(payrollEntryRepository.findAllByEmployeeIdAndStatusOrderByDueDateAsc(1L, PayrollStatus.PENDING))
            .thenReturn(listOf(pendingEntry))
        whenever(payrollEntryRepository.saveAll(any<List<PayrollEntry>>())).thenAnswer { it.arguments[0] }

        service.terminate(1L, TerminateEmployeeRequest(terminationDate = LocalDate.now(), cancelPendingPayroll = true))

        assertEquals(PayrollStatus.CANCELLED, pendingEntry.status)
        verify(payrollEntryRepository).saveAll(any<List<PayrollEntry>>())
    }

    // ─── calculateIndemnizacion ────────────────────────────────────────────────

    @Test
    fun `calculateIndemnizacion throws BadRequestException for non-PAYROLL employee`() {
        val contractor = makeEmployee(type = EmployeeType.CONTRACTOR)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))

        assertThrows<BadRequestException> { service.calculateIndemnizacion(1L) }
    }

    @Test
    fun `calculateIndemnizacion throws BadRequestException when no baseSalary`() {
        val employee = makeEmployee(baseSalary = null, hireDate = LocalDate.of(2024, 1, 1))
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        assertThrows<BadRequestException> { service.calculateIndemnizacion(1L) }
    }

    @Test
    fun `calculateIndemnizacion throws BadRequestException when no hireDate`() {
        val employee = makeEmployee(hireDate = null)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        assertThrows<BadRequestException> { service.calculateIndemnizacion(1L) }
    }

    @Test
    fun `calculateIndemnizacion returns computed liability`() {
        val hireDate = LocalDate.of(2023, 1, 1)
        val employee = makeEmployee(baseSalary = BigDecimal("3650.00"), hireDate = hireDate)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val result = service.calculateIndemnizacion(1L)

        assertTrue(result.daysWorked > 0)
        assertTrue(result.liability > BigDecimal.ZERO)
        assertEquals(employee.id!!, result.employeeId)
    }

    // ─── generatePayrollSchedule ───────────────────────────────────────────────

    @Test
    fun `generatePayrollSchedule throws BadRequestException for non-PAYROLL employee`() {
        val contractor = makeEmployee(type = EmployeeType.CONTRACTOR)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))

        assertThrows<BadRequestException> {
            service.generatePayrollSchedule(1L, GeneratePayrollScheduleRequest(year = 2026))
        }
    }

    @Test
    fun `generatePayrollSchedule throws BadRequestException when no baseSalary`() {
        val employee = makeEmployee(baseSalary = null)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        assertThrows<BadRequestException> {
            service.generatePayrollSchedule(1L, GeneratePayrollScheduleRequest(year = 2026))
        }
    }

    @Test
    fun `generatePayrollSchedule creates 14 entries and skips existing ones`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(payrollEntryRepository.existsByEmployeeIdAndYearAndPeriod(any(), any(), any())).thenReturn(false)
        whenever(payrollEntryRepository.save(any<PayrollEntry>())).thenAnswer { it.arguments[0] }
        whenever(payrollEntryRepository.findAllByEmployeeIdAndYearOrderByDueDateAsc(1L, 2026))
            .thenReturn(emptyList())

        service.generatePayrollSchedule(1L, GeneratePayrollScheduleRequest(year = 2026))

        // 12 months + BONO_14 + AGUINALDO = 14 entries attempted
        verify(payrollEntryRepository, org.mockito.kotlin.times(14)).save(any<PayrollEntry>())
    }

    @Test
    fun `generatePayrollSchedule skips already existing period`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(payrollEntryRepository.existsByEmployeeIdAndYearAndPeriod(1L, 2026, PayrollPeriod.JANUARY))
            .thenReturn(true)
        whenever(payrollEntryRepository.existsByEmployeeIdAndYearAndPeriod(any(), any(), any<PayrollPeriod>()))
            .thenReturn(false)
        whenever(payrollEntryRepository.existsByEmployeeIdAndYearAndPeriod(1L, 2026, PayrollPeriod.JANUARY))
            .thenReturn(true)
        whenever(payrollEntryRepository.save(any<PayrollEntry>())).thenAnswer { it.arguments[0] }
        whenever(payrollEntryRepository.findAllByEmployeeIdAndYearOrderByDueDateAsc(1L, 2026))
            .thenReturn(emptyList())

        service.generatePayrollSchedule(1L, GeneratePayrollScheduleRequest(year = 2026))

        // 13 entries created (January skipped)
        verify(payrollEntryRepository, org.mockito.kotlin.times(13)).save(any<PayrollEntry>())
    }

    // ─── payPayrollEntry ───────────────────────────────────────────────────────

    @Test
    fun `payPayrollEntry throws BadRequestException when entry is not PENDING`() {
        val employee = makeEmployee()
        val paid = makePayrollEntry(status = PayrollStatus.PAID)
        whenever(payrollEntryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(paid))

        assertThrows<BadRequestException> {
            service.payPayrollEntry(
                10L,
                RecordPayrollPaymentRequest(paymentDate = LocalDate.now(), bankAccountId = 20L),
            )
        }
    }

    @Test
    fun `payPayrollEntry throws ResourceNotFoundException when entry not found`() {
        whenever(payrollEntryRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.payPayrollEntry(
                99L,
                RecordPayrollPaymentRequest(paymentDate = LocalDate.now(), bankAccountId = 20L),
            )
        }
    }

    @Test
    fun `payPayrollEntry creates expense and payment, marks entry as PAID`() {
        val employee = makeEmployee()
        val entry = makePayrollEntry(employee = employee)
        val bankAccount = makeBankAccount()

        whenever(payrollEntryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(entry))
        whenever(bankAccountService.findEntityById(20L)).thenReturn(bankAccount)
        val savedExpense = Expense(
            supplierName = "Ana Lopez",
            category = com.insidehealthgt.hms.entity.ExpenseCategory.PAYROLL,
            amount = BigDecimal("5000.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "PAYROLL-1-2026-MARCH",
            status = com.insidehealthgt.hms.entity.ExpenseStatus.PAID,
            paidAmount = BigDecimal("5000.00"),
        ).also { it.id = 50L }
        whenever(expenseRepository.save(any<Expense>())).thenReturn(savedExpense)
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { it.arguments[0] }
        whenever(payrollEntryRepository.save(any<PayrollEntry>())).thenAnswer { it.arguments[0] }

        val result = service.payPayrollEntry(
            10L,
            RecordPayrollPaymentRequest(paymentDate = LocalDate.now(), bankAccountId = 20L),
        )

        assertEquals(PayrollStatus.PAID, entry.status)
        assertNotNull(entry.paidDate)
        assertEquals(50L, entry.expenseId)
        verify(expenseRepository).save(any())
        verify(expensePaymentRepository).save(any())
    }

    // ─── recordContractorPayment ───────────────────────────────────────────────

    @Test
    fun `recordContractorPayment throws BadRequestException for PAYROLL employee`() {
        val payrollEmployee = makeEmployee(type = EmployeeType.PAYROLL)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(payrollEmployee))

        assertThrows<BadRequestException> {
            service.recordContractorPayment(
                1L,
                RecordContractorPaymentRequest(
                    amount = BigDecimal("1000.00"),
                    paymentDate = LocalDate.now(),
                    invoiceNumber = "INV-001",
                ),
            )
        }
    }

    @Test
    fun `recordContractorPayment creates expense and payment for CONTRACTOR`() {
        val contractor = makeEmployee(
            type = EmployeeType.CONTRACTOR,
            baseSalary = null,
            contractedRate = BigDecimal("2000.00"),
        )
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(20L)).thenReturn(bankAccount)
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { invocation ->
            val e = invocation.getArgument<Expense>(0)
            e.also { it.id = 60L }
        }
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { it.arguments[0] }

        val result = service.recordContractorPayment(
            1L,
            RecordContractorPaymentRequest(
                amount = BigDecimal("2000.00"),
                paymentDate = LocalDate.now(),
                invoiceNumber = "INV-CONTRACTOR-001",
                bankAccountId = 20L,
            ),
        )

        assertNotNull(result)
        verify(expenseRepository).save(any())
        verify(expensePaymentRepository).save(any())
    }

    @Test
    fun `recordContractorPayment uses petty cash when no bankAccountId provided`() {
        val contractor = makeEmployee(
            type = EmployeeType.CONTRACTOR,
            baseSalary = null,
            contractedRate = BigDecimal("1500.00"),
        )
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))
        val pettyCash = makeBankAccount(id = 30L)
        whenever(bankAccountService.findPettyCashEntity()).thenReturn(pettyCash)
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { invocation ->
            invocation.getArgument<Expense>(0).also { it.id = 70L }
        }
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { it.arguments[0] }

        service.recordContractorPayment(
            1L,
            RecordContractorPaymentRequest(
                amount = BigDecimal("1500.00"),
                paymentDate = LocalDate.now(),
                invoiceNumber = "INV-002",
                bankAccountId = null,
            ),
        )

        verify(bankAccountService).findPettyCashEntity()
        verify(bankAccountService, never()).findEntityById(any())
    }

    @Test
    fun `recordContractorPayment throws BadRequestException when no petty cash and no bankAccountId`() {
        val contractor = makeEmployee(type = EmployeeType.CONTRACTOR)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(contractor))
        whenever(bankAccountService.findPettyCashEntity()).thenReturn(null)

        assertThrows<BadRequestException> {
            service.recordContractorPayment(
                1L,
                RecordContractorPaymentRequest(
                    amount = BigDecimal("1000.00"),
                    paymentDate = LocalDate.now(),
                    invoiceNumber = "INV-003",
                    bankAccountId = null,
                ),
            )
        }
    }

    // ─── create DOCTOR employee ──────────────────────────────────────────────

    @Test
    fun `create DOCTOR employee does not create salary history`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Dr. Garcia",
            employeeType = EmployeeType.DOCTOR,
            doctorFeeArrangement = DoctorFeeArrangement.HOSPITAL_BILLED,
            hospitalCommissionPct = BigDecimal("15.00"),
        )
        val saved = makeEmployee(
            type = EmployeeType.DOCTOR,
            baseSalary = null,
            contractedRate = null,
        )
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(saved)

        service.create(request)

        verify(salaryHistoryRepository, never()).save(any())
    }

    @Test
    fun `create DOCTOR employee stores fee arrangement and commission`() {
        val request = CreateTreasuryEmployeeRequest(
            fullName = "Dr. Martinez",
            employeeType = EmployeeType.DOCTOR,
            doctorFeeArrangement = DoctorFeeArrangement.EXTERNAL,
            hospitalCommissionPct = BigDecimal("20.00"),
            hireDate = LocalDate.of(2026, 1, 1),
        )
        val saved = TreasuryEmployee(
            fullName = "Dr. Martinez",
            employeeType = EmployeeType.DOCTOR,
            doctorFeeArrangement = DoctorFeeArrangement.EXTERNAL,
            hospitalCommissionPct = BigDecimal("20.00"),
            hireDate = LocalDate.of(2026, 1, 1),
        ).also { it.id = 5L }
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenReturn(saved)

        val result = service.create(request)

        assertEquals("Dr. Martinez", result.fullName)
        verify(employeeRepository).save(any())
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    fun `update modifies employee fields without changing type`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        whenever(employeeRepository.save(any<TreasuryEmployee>())).thenAnswer { it.arguments[0] }

        val request = UpdateTreasuryEmployeeRequest(
            fullName = "Ana Maria Lopez",
            position = "Senior Accountant",
            taxId = "123456-7",
        )

        val result = service.update(1L, request)

        assertEquals("Ana Maria Lopez", result.fullName)
        assertEquals("Senior Accountant", result.position)
        assertEquals("123456-7", result.taxId)
    }

    @Test
    fun `update throws ResourceNotFoundException for non-existing employee`() {
        whenever(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.update(99L, UpdateTreasuryEmployeeRequest(fullName = "Nobody"))
        }
    }

    // ─── getSalaryHistory ────────────────────────────────────────────────────

    @Test
    fun `getSalaryHistory returns ordered history entries`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val history1 = SalaryHistory(
            employee = employee,
            baseSalary = BigDecimal("4000.00"),
            effectiveFrom = LocalDate.of(2025, 1, 1),
        ).also {
            it.id = 1L
            it.effectiveTo = LocalDate.of(2025, 12, 31)
        }
        val history2 = SalaryHistory(
            employee = employee,
            baseSalary = BigDecimal("5000.00"),
            effectiveFrom = LocalDate.of(2026, 1, 1),
        ).also { it.id = 2L }

        whenever(salaryHistoryRepository.findAllByEmployeeIdOrderByEffectiveFromDesc(1L))
            .thenReturn(listOf(history2, history1))

        val result = service.getSalaryHistory(1L)

        assertEquals(2, result.size)
        assertEquals(BigDecimal("5000.00"), result[0].baseSalary)
        assertEquals(BigDecimal("4000.00"), result[1].baseSalary)
    }

    @Test
    fun `getSalaryHistory throws ResourceNotFoundException for non-existing employee`() {
        whenever(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getSalaryHistory(99L) }
    }

    // ─── getPaymentHistory ───────────────────────────────────────────────────

    @Test
    fun `getPaymentHistory includes paid payroll entries`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val paidEntry = makePayrollEntry(status = PayrollStatus.PAID).also {
            it.paidDate = LocalDate.of(2026, 3, 31)
        }
        whenever(payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(1L))
            .thenReturn(listOf(paidEntry))
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())

        val result = service.getPaymentHistory(1L)

        assertEquals(1, result.size)
        assertEquals(EmployeePaymentType.PAYROLL_ENTRY, result[0].type)
        assertTrue(result[0].reference!!.startsWith("PAYROLL-"))
    }

    @Test
    fun `getPaymentHistory excludes PENDING payroll entries`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val pendingEntry = makePayrollEntry(status = PayrollStatus.PENDING)
        whenever(payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(1L))
            .thenReturn(listOf(pendingEntry))
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())

        val result = service.getPaymentHistory(1L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPaymentHistory excludes CANCELLED payroll entries`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        val cancelledEntry = makePayrollEntry(status = PayrollStatus.CANCELLED)
        whenever(payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(1L))
            .thenReturn(listOf(cancelledEntry))
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())

        val result = service.getPaymentHistory(1L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPaymentHistory includes contractor payments and deduplicates payroll expenses`() {
        val employee = makeEmployee()
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        whenever(payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(1L))
            .thenReturn(emptyList())

        val contractorExpense = Expense(
            supplierName = "Ana Lopez",
            category = ExpenseCategory.PAYROLL,
            amount = BigDecimal("2000.00"),
            expenseDate = LocalDate.of(2026, 2, 15),
            invoiceNumber = "INV-CONTRACTOR-001",
            status = ExpenseStatus.PAID,
            paidAmount = BigDecimal("2000.00"),
            treasuryEmployeeId = 1L,
        ).also { it.id = 60L }

        // This expense should be deduplicated (payroll reference pattern)
        val payrollExpense = Expense(
            supplierName = "Ana Lopez",
            category = ExpenseCategory.PAYROLL,
            amount = BigDecimal("5000.00"),
            expenseDate = LocalDate.of(2026, 3, 31),
            invoiceNumber = "PAYROLL-1-2026-MARCH",
            status = ExpenseStatus.PAID,
            paidAmount = BigDecimal("5000.00"),
            treasuryEmployeeId = 1L,
        ).also { it.id = 61L }

        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(listOf(contractorExpense, payrollExpense))

        val result = service.getPaymentHistory(1L)

        assertEquals(1, result.size)
        assertEquals(EmployeePaymentType.CONTRACTOR_PAYMENT, result[0].type)
        assertEquals("INV-CONTRACTOR-001", result[0].reference)
    }

    @Test
    fun `getPaymentHistory throws ResourceNotFoundException for non-existing employee`() {
        whenever(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getPaymentHistory(99L) }
    }
}
