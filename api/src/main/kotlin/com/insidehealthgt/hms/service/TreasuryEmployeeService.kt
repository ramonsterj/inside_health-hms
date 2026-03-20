package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.request.GeneratePayrollScheduleRequest
import com.insidehealthgt.hms.dto.request.RecordContractorPaymentRequest
import com.insidehealthgt.hms.dto.request.RecordPayrollPaymentRequest
import com.insidehealthgt.hms.dto.request.TerminateEmployeeRequest
import com.insidehealthgt.hms.dto.request.UpdateSalaryRequest
import com.insidehealthgt.hms.dto.request.UpdateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.response.EmployeePaymentHistoryResponse
import com.insidehealthgt.hms.dto.response.EmployeePaymentType
import com.insidehealthgt.hms.dto.response.ExpenseResponse
import com.insidehealthgt.hms.dto.response.IndemnizacionResponse
import com.insidehealthgt.hms.dto.response.PayrollEntryResponse
import com.insidehealthgt.hms.dto.response.SalaryHistoryResponse
import com.insidehealthgt.hms.dto.response.TreasuryEmployeeResponse
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollPeriod
import com.insidehealthgt.hms.entity.PayrollStatus
import com.insidehealthgt.hms.entity.SalaryHistory
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.DoctorFeeRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.PayrollEntryRepository
import com.insidehealthgt.hms.repository.SalaryHistoryRepository
import com.insidehealthgt.hms.repository.TreasuryEmployeeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Suppress("TooManyFunctions")
@Service
class TreasuryEmployeeService(
    private val employeeRepository: TreasuryEmployeeRepository,
    private val salaryHistoryRepository: SalaryHistoryRepository,
    private val payrollEntryRepository: PayrollEntryRepository,
    private val doctorFeeRepository: DoctorFeeRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseService: ExpenseService,
    private val bankAccountService: BankAccountService,
    private val userRepository: UserRepository,
) {

    companion object {
        private const val PAYROLL_REF_PREFIX = "PAYROLL-"
        private const val DAYS_IN_YEAR = 365
        private const val INDEMNIZACION_DIVISION_SCALE = 4
        private const val BONO_14_MONTH = 7
        private const val BONO_14_DAY = 15
        private const val AGUINALDO_MONTH = 12
        private const val AGUINALDO_DAY = 15

        fun computeIndemnizacionLiability(
            baseSalary: BigDecimal,
            hireDate: LocalDate,
            asOfDate: LocalDate,
        ): BigDecimal {
            val daysWorked = ChronoUnit.DAYS.between(hireDate, asOfDate)
            return baseSalary.multiply(
                BigDecimal(daysWorked).divide(
                    BigDecimal(DAYS_IN_YEAR),
                    INDEMNIZACION_DIVISION_SCALE,
                    RoundingMode.HALF_UP,
                ),
            ).setScale(2, RoundingMode.HALF_UP)
        }
    }

    @Transactional(readOnly = true)
    fun findAll(type: EmployeeType?, activeOnly: Boolean, search: String?): List<TreasuryEmployeeResponse> {
        val employees = when {
            activeOnly && type != null -> employeeRepository.findAllByActiveTrueAndEmployeeTypeOrderByFullNameAsc(type)
            activeOnly -> employeeRepository.findAllByActiveTrueOrderByFullNameAsc()
            type != null -> employeeRepository.findAllByEmployeeTypeOrderByFullNameAsc(type)
            else -> employeeRepository.findAllByOrderByFullNameAsc()
        }.let { list ->
            if (!search.isNullOrBlank()) {
                val lower = search.lowercase()
                list.filter { it.fullName.lowercase().contains(lower) }
            } else {
                list
            }
        }
        val userIds = employees.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.toSet()
        val usersById = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        }
        return employees.map { buildResponse(it, usersById) }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): TreasuryEmployeeResponse {
        val employee = findEntityById(id)
        return buildResponse(employee)
    }

    @Transactional
    fun create(request: CreateTreasuryEmployeeRequest): TreasuryEmployeeResponse {
        validateEmployeeTypeFields(request.employeeType, request.baseSalary, request.contractedRate)
        val employee = TreasuryEmployee(
            fullName = request.fullName,
            employeeType = request.employeeType,
            taxId = request.taxId?.takeIf { it.isNotBlank() },
            position = request.position?.takeIf { it.isNotBlank() },
            baseSalary = request.baseSalary,
            contractedRate = request.contractedRate,
            doctorFeeArrangement = request.doctorFeeArrangement,
            hospitalCommissionPct = request.hospitalCommissionPct ?: BigDecimal.ZERO,
            hireDate = request.hireDate,
            userId = request.userId,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val saved = employeeRepository.save(employee)

        // Create initial salary history for PAYROLL employees
        if (request.employeeType == EmployeeType.PAYROLL && request.baseSalary != null) {
            val history = SalaryHistory(
                employee = saved,
                baseSalary = request.baseSalary,
                effectiveFrom = request.hireDate ?: LocalDate.now(),
            )
            salaryHistoryRepository.save(history)
        }

        return buildResponse(saved)
    }

    @Transactional
    fun update(id: Long, request: UpdateTreasuryEmployeeRequest): TreasuryEmployeeResponse {
        val employee = findEntityById(id)
        employee.fullName = request.fullName
        employee.taxId = request.taxId?.takeIf { it.isNotBlank() }
        employee.position = request.position?.takeIf { it.isNotBlank() }
        employee.contractedRate = request.contractedRate
        employee.doctorFeeArrangement = request.doctorFeeArrangement
        employee.hospitalCommissionPct = request.hospitalCommissionPct ?: BigDecimal.ZERO
        employee.hireDate = request.hireDate
        employee.userId = request.userId
        employee.notes = request.notes?.takeIf { it.isNotBlank() }
        val saved = employeeRepository.save(employee)
        return buildResponse(saved)
    }

    @Transactional
    fun updateSalary(id: Long, request: UpdateSalaryRequest): TreasuryEmployeeResponse {
        val employee = findEntityById(id)
        if (employee.employeeType != EmployeeType.PAYROLL) {
            throw BadRequestException("Salary updates are only applicable to PAYROLL employees")
        }

        // Close current open salary history
        val currentHistory = salaryHistoryRepository.findByEmployeeIdAndEffectiveToIsNull(id)
        if (currentHistory != null) {
            if (!request.effectiveFrom.isAfter(currentHistory.effectiveFrom)) {
                throw BadRequestException(
                    "effectiveFrom must be after the current salary's effective date (${currentHistory.effectiveFrom})",
                )
            }
            currentHistory.effectiveTo = request.effectiveFrom.minusDays(1)
            salaryHistoryRepository.saveAndFlush(currentHistory)
        }

        // Create new salary history
        val newHistory = SalaryHistory(
            employee = employee,
            baseSalary = request.newSalary,
            effectiveFrom = request.effectiveFrom,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        salaryHistoryRepository.save(newHistory)

        // Update employee base salary cache
        employee.baseSalary = request.newSalary
        val saved = employeeRepository.save(employee)

        // Update only PENDING payroll entries due on or after the effective date
        val pendingEntries = payrollEntryRepository.findAllByEmployeeIdAndStatusOrderByDueDateAsc(
            id,
            PayrollStatus.PENDING,
        ).filter { it.dueDate >= request.effectiveFrom }
        pendingEntries.forEach { entry ->
            entry.baseSalary = request.newSalary
            entry.grossAmount = request.newSalary
        }
        if (pendingEntries.isNotEmpty()) {
            payrollEntryRepository.saveAll(pendingEntries)
        }

        return buildResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getSalaryHistory(id: Long): List<SalaryHistoryResponse> {
        findEntityById(id) // verify exists
        return salaryHistoryRepository.findAllByEmployeeIdOrderByEffectiveFromDesc(id)
            .map { SalaryHistoryResponse.from(it) }
    }

    @Transactional
    fun generatePayrollSchedule(id: Long, request: GeneratePayrollScheduleRequest): List<PayrollEntryResponse> {
        val employee = findEntityById(id)
        if (employee.employeeType != EmployeeType.PAYROLL) {
            throw BadRequestException("Payroll schedule generation is only applicable to PAYROLL employees")
        }
        val salary = employee.baseSalary
            ?: throw BadRequestException("Employee has no base salary set")

        val year = request.year
        val periodsToCreate = buildPayrollPeriods(year)

        periodsToCreate.forEach { (period, dueDate, label) ->
            if (!payrollEntryRepository.existsByEmployeeIdAndYearAndPeriod(id, year, period)) {
                val entry = PayrollEntry(
                    employee = employee,
                    year = year,
                    period = period,
                    periodLabel = label,
                    baseSalary = salary,
                    grossAmount = salary,
                    dueDate = dueDate,
                )
                payrollEntryRepository.save(entry)
            }
        }

        return payrollEntryRepository.findAllByEmployeeIdAndYearOrderByDueDateAsc(id, year)
            .map { PayrollEntryResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getPayrollEntries(employeeId: Long): List<PayrollEntryResponse> {
        findEntityById(employeeId) // verify exists
        return payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(employeeId)
            .map { PayrollEntryResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getPayrollEntriesForYear(employeeId: Long, year: Int): List<PayrollEntryResponse> {
        findEntityById(employeeId) // verify exists
        return payrollEntryRepository.findAllByEmployeeIdAndYearOrderByDueDateAsc(employeeId, year)
            .map { PayrollEntryResponse.from(it) }
    }

    @Transactional
    fun payPayrollEntry(entryId: Long, request: RecordPayrollPaymentRequest): PayrollEntryResponse {
        val entry = payrollEntryRepository.findByIdForUpdate(entryId)
            .orElseThrow { ResourceNotFoundException("Payroll entry not found with id: $entryId") }
        if (entry.status != PayrollStatus.PENDING) {
            throw BadRequestException("Payroll entry is not in PENDING status")
        }
        val reference = "$PAYROLL_REF_PREFIX${entry.employee.id}-${entry.year}-${entry.period}"

        // Auto-create expense
        val savedExpense = expenseService.createPaidExpense(
            CreatePaidExpenseCommand(
                supplierName = entry.employee.fullName,
                category = ExpenseCategory.PAYROLL,
                amount = entry.grossAmount,
                expenseDate = request.paymentDate,
                invoiceNumber = reference,
                bankAccountId = request.bankAccountId,
                paymentDate = request.paymentDate,
                paymentReference = reference,
                treasuryEmployeeId = entry.employee.id,
                notes = request.notes,
            ),
        )

        // Mark entry as paid
        entry.status = PayrollStatus.PAID
        entry.paidDate = request.paymentDate
        entry.expenseId = savedExpense.id
        val savedEntry = payrollEntryRepository.save(entry)

        return PayrollEntryResponse.from(savedEntry)
    }

    @Transactional
    fun recordContractorPayment(id: Long, request: RecordContractorPaymentRequest): ExpenseResponse {
        val employee = findEntityById(id)
        if (employee.employeeType != EmployeeType.CONTRACTOR) {
            throw BadRequestException("Contractor payments are only applicable to CONTRACTOR employees")
        }
        val bankAccount = if (request.bankAccountId != null) {
            bankAccountService.findEntityById(request.bankAccountId)
        } else {
            bankAccountService.findPettyCashEntity()
                ?: throw BadRequestException("No petty cash account found")
        }

        val savedExpense = expenseService.createPaidExpense(
            CreatePaidExpenseCommand(
                supplierName = employee.fullName,
                category = ExpenseCategory.PAYROLL,
                amount = request.amount,
                expenseDate = request.paymentDate,
                invoiceNumber = request.invoiceNumber,
                bankAccountId = bankAccount.id!!,
                paymentDate = request.paymentDate,
                paymentReference = request.invoiceNumber,
                treasuryEmployeeId = id,
                notes = request.notes,
            ),
        )
        return ExpenseResponse.from(savedExpense)
    }

    @Transactional
    fun terminate(id: Long, request: TerminateEmployeeRequest): TreasuryEmployeeResponse {
        val employee = findEntityById(id)
        if (!employee.active) {
            throw BadRequestException("Employee is already terminated")
        }
        employee.active = false
        employee.terminationDate = request.terminationDate
        employee.terminationReason = request.terminationReason?.takeIf { it.isNotBlank() }

        if (request.cancelPendingPayroll == true) {
            val pendingEntries = payrollEntryRepository.findAllByEmployeeIdAndStatusOrderByDueDateAsc(
                id,
                PayrollStatus.PENDING,
            )
            pendingEntries.forEach { it.status = PayrollStatus.CANCELLED }
            if (pendingEntries.isNotEmpty()) {
                payrollEntryRepository.saveAll(pendingEntries)
            }
        }

        val saved = employeeRepository.save(employee)
        return buildResponse(saved)
    }

    @Suppress("ThrowsCount")
    @Transactional(readOnly = true)
    fun calculateIndemnizacion(id: Long): IndemnizacionResponse {
        val employee = findEntityById(id)
        if (employee.employeeType != EmployeeType.PAYROLL) {
            throw BadRequestException("Indemnización calculation is only applicable to PAYROLL employees")
        }
        val salary = employee.baseSalary
            ?: throw BadRequestException("Employee has no base salary set")
        val hireDate = employee.hireDate
            ?: throw BadRequestException("Employee has no hire date set")

        val today = LocalDate.now()
        val daysWorked = ChronoUnit.DAYS.between(hireDate, today)
        val liability = computeIndemnizacionLiability(salary, hireDate, today)

        return IndemnizacionResponse(
            employeeId = employee.id!!,
            employeeName = employee.fullName,
            baseSalary = salary,
            hireDate = hireDate,
            daysWorked = daysWorked,
            liability = liability,
            asOfDate = today,
        )
    }

    @Transactional(readOnly = true)
    fun getPaymentHistory(employeeId: Long): List<EmployeePaymentHistoryResponse> {
        findEntityById(employeeId) // verify exists
        val result = mutableListOf<EmployeePaymentHistoryResponse>()

        // Payroll entries (PAYROLL employees)
        payrollEntryRepository.findAllByEmployeeIdOrderByYearDescDueDateAsc(employeeId)
            .filter { it.status == PayrollStatus.PAID }
            .forEach { entry ->
                result.add(
                    EmployeePaymentHistoryResponse(
                        type = EmployeePaymentType.PAYROLL_ENTRY,
                        amount = entry.grossAmount,
                        date = entry.paidDate ?: entry.dueDate,
                        reference = "$PAYROLL_REF_PREFIX$employeeId-${entry.year}-${entry.period}",
                        status = entry.status.name,
                        relatedEntityId = entry.id!!,
                        createdAt = entry.createdAt,
                    ),
                )
            }

        // Contractor/payroll expenses linked to this employee
        expenseRepository.findAll(
            org.springframework.data.jpa.domain.Specification { root, _, cb ->
                cb.and(
                    cb.equal(root.get<Long>("treasuryEmployeeId"), employeeId),
                    cb.equal(root.get<ExpenseCategory>("category"), ExpenseCategory.PAYROLL),
                )
            },
        ).forEach { expense ->
            // Only add if not already captured as a payroll entry
            val isPayrollEntry = expense.invoiceNumber.startsWith("$PAYROLL_REF_PREFIX$employeeId-")
            if (!isPayrollEntry) {
                result.add(
                    EmployeePaymentHistoryResponse(
                        type = EmployeePaymentType.CONTRACTOR_PAYMENT,
                        amount = expense.amount,
                        date = expense.expenseDate,
                        reference = expense.invoiceNumber,
                        status = expense.status.name,
                        relatedEntityId = expense.id!!,
                        createdAt = expense.createdAt,
                    ),
                )
            }
        }

        // Doctor fee settlements
        doctorFeeRepository.findAllByTreasuryEmployeeIdAndStatus(employeeId, DoctorFeeStatus.PAID)
            .forEach { fee ->
                result.add(
                    EmployeePaymentHistoryResponse(
                        type = EmployeePaymentType.DOCTOR_FEE_SETTLEMENT,
                        amount = fee.netAmount,
                        date = fee.feeDate,
                        reference = fee.doctorInvoiceNumber,
                        status = fee.status.name,
                        relatedEntityId = fee.id!!,
                        createdAt = fee.createdAt,
                    ),
                )
            }

        return result.sortedByDescending { it.date }
    }

    fun findEntityById(id: Long): TreasuryEmployee = employeeRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Treasury employee not found with id: $id") }

    private fun isIndemnizacionEligible(employee: TreasuryEmployee): Boolean =
        employee.employeeType == EmployeeType.PAYROLL &&
            employee.active &&
            employee.baseSalary != null &&
            employee.hireDate != null

    private fun buildResponse(
        employee: TreasuryEmployee,
        usersById: Map<Long, User> = emptyMap(),
    ): TreasuryEmployeeResponse {
        val indemnizacionLiability = if (isIndemnizacionEligible(employee)) {
            computeIndemnizacionLiability(employee.baseSalary!!, employee.hireDate!!, LocalDate.now())
        } else {
            null
        }
        val createdByUser = employee.createdBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        val updatedByUser = employee.updatedBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        return TreasuryEmployeeResponse.from(employee, indemnizacionLiability, createdByUser, updatedByUser)
    }

    private fun validateEmployeeTypeFields(type: EmployeeType, baseSalary: BigDecimal?, contractedRate: BigDecimal?) {
        if (type == EmployeeType.PAYROLL && baseSalary == null) {
            throw BadRequestException("Base salary is required for PAYROLL employees")
        }
        if (type == EmployeeType.CONTRACTOR && contractedRate == null) {
            throw BadRequestException("Contracted rate is required for CONTRACTOR employees")
        }
    }

    private fun buildPayrollPeriods(year: Int): List<Triple<PayrollPeriod, LocalDate, String>> {
        val monthPeriods = listOf(
            Triple(PayrollPeriod.JANUARY, Month.JANUARY, "January"),
            Triple(PayrollPeriod.FEBRUARY, Month.FEBRUARY, "February"),
            Triple(PayrollPeriod.MARCH, Month.MARCH, "March"),
            Triple(PayrollPeriod.APRIL, Month.APRIL, "April"),
            Triple(PayrollPeriod.MAY, Month.MAY, "May"),
            Triple(PayrollPeriod.JUNE, Month.JUNE, "June"),
            Triple(PayrollPeriod.JULY, Month.JULY, "July"),
            Triple(PayrollPeriod.AUGUST, Month.AUGUST, "August"),
            Triple(PayrollPeriod.SEPTEMBER, Month.SEPTEMBER, "September"),
            Triple(PayrollPeriod.OCTOBER, Month.OCTOBER, "October"),
            Triple(PayrollPeriod.NOVEMBER, Month.NOVEMBER, "November"),
            Triple(PayrollPeriod.DECEMBER, Month.DECEMBER, "December"),
        ).map { (period, month, name) ->
            Triple(period, YearMonth.of(year, month).atEndOfMonth(), "$name $year")
        }

        val bonoPeriods = listOf(
            Triple(PayrollPeriod.BONO_14, LocalDate.of(year, BONO_14_MONTH, BONO_14_DAY), "Bono 14 $year"),
            Triple(PayrollPeriod.AGUINALDO, LocalDate.of(year, AGUINALDO_MONTH, AGUINALDO_DAY), "Aguinaldo $year"),
        )

        return monthPeriods + bonoPeriods
    }
}
