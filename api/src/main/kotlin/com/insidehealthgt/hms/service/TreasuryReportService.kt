package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.BankAccountSummaryItem
import com.insidehealthgt.hms.dto.response.BankAccountSummaryResponse
import com.insidehealthgt.hms.dto.response.BankBalanceSnapshot
import com.insidehealthgt.hms.dto.response.EmployeeCompensationItem
import com.insidehealthgt.hms.dto.response.EmployeeCompensationResponse
import com.insidehealthgt.hms.dto.response.IndemnizacionLiabilityItem
import com.insidehealthgt.hms.dto.response.IndemnizacionLiabilityResponse
import com.insidehealthgt.hms.dto.response.MonthlyCategoryBreakdown
import com.insidehealthgt.hms.dto.response.MonthlyPaymentReportResponse
import com.insidehealthgt.hms.dto.response.RecentTransactionItem
import com.insidehealthgt.hms.dto.response.ReconciliationSummaryItem
import com.insidehealthgt.hms.dto.response.ReconciliationSummaryResponse
import com.insidehealthgt.hms.dto.response.TreasuryDashboardResponse
import com.insidehealthgt.hms.dto.response.UpcomingPayableItem
import com.insidehealthgt.hms.dto.response.UpcomingPaymentsResponse
import com.insidehealthgt.hms.entity.BankStatementStatus
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollStatus
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.repository.BankAccountRepository
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.IncomeRepository
import com.insidehealthgt.hms.repository.PayrollEntryRepository
import com.insidehealthgt.hms.repository.TreasuryEmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Suppress("TooManyFunctions")
@Service
class TreasuryReportService(
    private val bankAccountRepository: BankAccountRepository,
    private val bankAccountService: BankAccountService,
    private val expenseRepository: ExpenseRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val incomeRepository: IncomeRepository,
    private val payrollEntryRepository: PayrollEntryRepository,
    private val employeeRepository: TreasuryEmployeeRepository,
    private val bankStatementRepository: BankStatementRepository,
) {

    companion object {
        private const val DASHBOARD_PENDING_DAYS = 30
        private const val DASHBOARD_UPCOMING_DAYS = 7
        private const val DEFAULT_WINDOW_DAYS = 30
        private const val RECENT_TRANSACTION_LIMIT = 10
        private const val COVERAGE_SCALE = 2
        private const val PERCENTAGE_MULTIPLIER = 100
        private val PENDING_STATUSES = listOf(ExpenseStatus.PENDING, ExpenseStatus.PARTIALLY_PAID)
    }

    @Transactional(readOnly = true)
    fun getDashboard(): TreasuryDashboardResponse {
        val today = LocalDate.now()

        // Bank balances
        val accounts = bankAccountRepository.findAllByActiveTrueOrderByNameAsc()
        val bankBalances = accounts.map { account ->
            BankBalanceSnapshot(
                bankAccountId = account.id!!,
                name = account.name,
                bookBalance = bankAccountService.computeBookBalance(account),
                currency = account.currency,
                active = account.active,
            )
        }

        // Pending payables: expenses with PENDING/PARTIALLY_PAID status and dueDate within 30 days
        val pendingExpenses = expenseRepository.findAllByStatusInAndDueDateBetween(
            PENDING_STATUSES,
            today,
            today.plusDays(DASHBOARD_PENDING_DAYS.toLong()),
        )
        val pendingPayroll = payrollEntryRepository.findAllByStatusAndDueDateBetween(
            PayrollStatus.PENDING,
            today,
            today.plusDays(DASHBOARD_PENDING_DAYS.toLong()),
        )
        val pendingPayablesCount = pendingExpenses.size + pendingPayroll.size
        val pendingExpensesTotal = pendingExpenses.sumOf { it.amount.subtract(it.paidAmount) }
        val pendingPayrollTotal = pendingPayroll.sumOf { it.grossAmount }
        val pendingPayablesTotal = pendingExpensesTotal.add(pendingPayrollTotal)

        // Next 7-day obligations
        val upcomingExpenses = expenseRepository.findAllByStatusInAndDueDateBetween(
            PENDING_STATUSES,
            today,
            today.plusDays(DASHBOARD_UPCOMING_DAYS.toLong()),
        )
        val upcomingPayroll = payrollEntryRepository.findAllByStatusAndDueDateBetween(
            PayrollStatus.PENDING,
            today,
            today.plusDays(DASHBOARD_UPCOMING_DAYS.toLong()),
        )
        val next7DayObligations = buildPayableItems(upcomingExpenses, upcomingPayroll)
        val next7DayTotal = next7DayObligations.sumOf { it.amount }

        return TreasuryDashboardResponse(
            bankBalances = bankBalances,
            pendingPayablesCount = pendingPayablesCount,
            pendingPayablesTotal = pendingPayablesTotal,
            next7DayObligations = next7DayObligations,
            next7DayTotal = next7DayTotal,
        )
    }

    @Transactional(readOnly = true)
    fun getMonthlyReport(from: LocalDate, to: LocalDate): MonthlyPaymentReportResponse {
        val payments = expensePaymentRepository.findAllByPaymentDateBetween(from, to)
        val expensesByCategory = payments.groupBy { it.expense.category }
            .map { (category, items) ->
                MonthlyCategoryBreakdown(
                    category = category.name,
                    label = category.name,
                    total = items.sumOf { it.amount },
                    count = items.size,
                )
            }
            .sortedByDescending { it.total }
        val totalExpenses = expensesByCategory.sumOf { it.total }

        val incomeRecords = incomeRepository.findAllByIncomeDateBetween(from, to)
        val incomeByCategory = incomeRecords.groupBy { it.category }
            .map { (category, items) ->
                MonthlyCategoryBreakdown(
                    category = category.name,
                    label = category.name,
                    total = items.sumOf { it.amount },
                    count = items.size,
                )
            }
            .sortedByDescending { it.total }
        val totalIncome = incomeByCategory.sumOf { it.total }

        return MonthlyPaymentReportResponse(
            from = from,
            to = to,
            expensesByCategory = expensesByCategory,
            totalExpenses = totalExpenses,
            incomeByCategory = incomeByCategory,
            totalIncome = totalIncome,
            netBalance = totalIncome.subtract(totalExpenses),
        )
    }

    @Transactional(readOnly = true)
    fun getUpcomingPayments(windowDays: Int = DEFAULT_WINDOW_DAYS): UpcomingPaymentsResponse {
        val today = LocalDate.now()
        val to = today.plusDays(windowDays.toLong())

        val expenses = expenseRepository.findAllByStatusInAndDueDateBetween(
            PENDING_STATUSES,
            today,
            to,
        )
        val payrollEntries = payrollEntryRepository.findAllByStatusAndDueDateBetween(
            PayrollStatus.PENDING,
            today,
            to,
        )

        val allItems = buildPayableItems(expenses, payrollEntries)

        return UpcomingPaymentsResponse(
            windowDays = windowDays,
            from = today,
            to = to,
            items = allItems,
            totalAmount = allItems.sumOf { it.amount },
            expenseCount = expenses.size,
            payrollCount = payrollEntries.size,
        )
    }

    @Transactional(readOnly = true)
    fun getBankAccountSummary(): BankAccountSummaryResponse {
        val accounts = bankAccountRepository.findAllByOrderByNameAsc()
        val summaryItems = accounts.map { account ->
            val bookBalance = bankAccountService.computeBookBalance(account)
            val lastStatement = bankStatementRepository.findTopByBankAccountIdOrderByStatementDateDesc(account.id!!)

            // Recent 10 transactions: merge expense payments + income, sort by date desc, take 10
            val recentPayments = expensePaymentRepository
                .findTop10ByBankAccountIdOrderByPaymentDateDesc(account.id!!)
                .map { payment ->
                    RecentTransactionItem(
                        type = "EXPENSE_PAYMENT",
                        date = payment.paymentDate,
                        description = payment.expense.supplierName,
                        amount = payment.amount.negate(),
                        reference = payment.reference,
                    )
                }
            val recentIncome = incomeRepository
                .findTop10ByBankAccountIdOrderByIncomeDateDesc(account.id!!)
                .map { income ->
                    RecentTransactionItem(
                        type = "INCOME",
                        date = income.incomeDate,
                        description = income.description,
                        amount = income.amount,
                        reference = income.reference,
                    )
                }
            val recentTransactions = (recentPayments + recentIncome)
                .sortedByDescending { it.date }
                .take(RECENT_TRANSACTION_LIMIT)

            BankAccountSummaryItem(
                bankAccountId = account.id!!,
                name = account.name,
                bankName = account.bankName,
                accountType = account.accountType.name,
                currency = account.currency,
                openingBalance = account.openingBalance,
                bookBalance = bookBalance,
                lastStatementDate = lastStatement?.statementDate,
                lastStatementBalance = null,
                active = account.active,
                recentTransactions = recentTransactions,
            )
        }
        val totalBookBalance = summaryItems.sumOf { it.bookBalance }

        return BankAccountSummaryResponse(
            accounts = summaryItems,
            totalBookBalance = totalBookBalance,
        )
    }

    @Transactional(readOnly = true)
    fun getEmployeeCompensation(year: Int): EmployeeCompensationResponse {
        val employees = employeeRepository.findAllByActiveTrueOrderByFullNameAsc()
        val yearStart = LocalDate.ofYearDay(year, 1)
        val yearEnd = java.time.YearMonth.of(year, java.time.Month.DECEMBER).atEndOfMonth()

        // Pre-fetch all data to avoid N+1 queries
        val paidEntriesByEmployee = payrollEntryRepository.findAllByStatusAndPaidDateBetween(
            PayrollStatus.PAID,
            yearStart,
            yearEnd,
        ).groupBy { it.employee.id }

        val pendingEntriesByEmployee = payrollEntryRepository.findAllByStatusOrderByDueDateAsc(
            PayrollStatus.PENDING,
        ).groupBy { it.employee.id }

        val payrollPaymentsInYear = expensePaymentRepository.findAllByPaymentDateBetween(yearStart, yearEnd)
            .filter { it.expense.category == ExpenseCategory.PAYROLL }

        val pendingPayrollExpenses = expenseRepository.findAll(
            org.springframework.data.jpa.domain.Specification { root, _, cb ->
                cb.and(
                    root.get<Long>("treasuryEmployeeId").isNotNull,
                    root.get<ExpenseStatus>("status").`in`(PENDING_STATUSES),
                )
            },
        ).groupBy { it.treasuryEmployeeId }

        val compensationItems = employees.map { employee ->
            buildCompensationItem(
                employee,
                paidEntriesByEmployee,
                pendingEntriesByEmployee,
                payrollPaymentsInYear,
                pendingPayrollExpenses,
            )
        }

        return EmployeeCompensationResponse(
            year = year,
            employees = compensationItems,
            totalYtdPayments = compensationItems.sumOf { it.ytdPayments },
            totalPending = compensationItems.sumOf { it.pendingAmount },
        )
    }

    @Transactional(readOnly = true)
    fun getIndemnizacionLiability(): IndemnizacionLiabilityResponse {
        val today = LocalDate.now()
        val employees = employeeRepository.findAllByActiveTrueOrderByFullNameAsc()
            .filter {
                it.employeeType == EmployeeType.PAYROLL &&
                    it.hireDate != null &&
                    it.baseSalary != null
            }

        val items = employees.map { employee ->
            val daysWorked = ChronoUnit.DAYS.between(employee.hireDate!!, today)
            val liability = TreasuryEmployeeService.computeIndemnizacionLiability(
                employee.baseSalary!!,
                employee.hireDate!!,
                today,
            )
            IndemnizacionLiabilityItem(
                employeeId = employee.id!!,
                fullName = employee.fullName,
                position = employee.position,
                hireDate = employee.hireDate!!,
                tenureDays = daysWorked,
                currentSalary = employee.baseSalary!!,
                liability = liability,
            )
        }

        return IndemnizacionLiabilityResponse(
            asOfDate = today,
            employees = items,
            grandTotal = items.sumOf { it.liability },
        )
    }

    @Transactional(readOnly = true)
    fun getReconciliationSummary(): ReconciliationSummaryResponse {
        val accounts = bankAccountRepository.findAllByActiveTrueOrderByNameAsc()

        val items = accounts.map { account ->
            val statements = bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(account.id!!)
            val totalStatements = statements.size
            val totalRows = statements.sumOf { it.totalRows }
            val matchedCount = statements.sumOf { it.matchedCount }
            val unmatchedCount = statements.sumOf { it.unmatchedCount }
            val acknowledgedCount = statements.sumOf { it.acknowledgedCount }
            val coveragePct = if (totalRows > 0) {
                BigDecimal(matchedCount + acknowledgedCount)
                    .multiply(BigDecimal(PERCENTAGE_MULTIPLIER))
                    .divide(BigDecimal(totalRows), COVERAGE_SCALE, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            val lastCompleted = statements.firstOrNull { it.status == BankStatementStatus.COMPLETED }

            ReconciliationSummaryItem(
                bankAccountId = account.id!!,
                bankAccountName = account.name,
                lastReconciliationDate = lastCompleted?.statementDate,
                lastStatementStatus = statements.firstOrNull()?.status?.name,
                totalStatements = totalStatements,
                totalRows = totalRows,
                matchedCount = matchedCount,
                unmatchedCount = unmatchedCount,
                acknowledgedCount = acknowledgedCount,
                coveragePct = coveragePct,
            )
        }

        return ReconciliationSummaryResponse(accounts = items)
    }

    private fun buildCompensationItem(
        employee: TreasuryEmployee,
        paidEntriesByEmployee: Map<Long?, List<PayrollEntry>>,
        pendingEntriesByEmployee: Map<Long?, List<PayrollEntry>>,
        payrollPaymentsInYear: List<ExpensePayment>,
        pendingPayrollExpenses: Map<Long?, List<Expense>>,
    ): EmployeeCompensationItem {
        val ytdPayments: BigDecimal
        val pendingAmount: BigDecimal

        when (employee.employeeType) {
            EmployeeType.PAYROLL -> {
                val paidEntries = paidEntriesByEmployee[employee.id] ?: emptyList()
                ytdPayments = paidEntries.sumOf { it.grossAmount }

                val pendingEntries = pendingEntriesByEmployee[employee.id] ?: emptyList()
                pendingAmount = pendingEntries.sumOf { it.grossAmount }
            }

            EmployeeType.CONTRACTOR -> {
                val paidPayments = payrollPaymentsInYear.filter {
                    it.expense.treasuryEmployeeId == employee.id
                }
                ytdPayments = paidPayments.sumOf { it.amount }

                val empPendingExpenses = pendingPayrollExpenses[employee.id] ?: emptyList()
                pendingAmount = empPendingExpenses.sumOf { it.amount.subtract(it.paidAmount) }
            }

            EmployeeType.DOCTOR -> {
                ytdPayments = BigDecimal.ZERO
                pendingAmount = BigDecimal.ZERO
            }
        }

        return EmployeeCompensationItem(
            employeeId = employee.id!!,
            fullName = employee.fullName,
            employeeType = employee.employeeType.name,
            position = employee.position,
            compensation = employee.baseSalary ?: employee.contractedRate,
            ytdPayments = ytdPayments,
            pendingAmount = pendingAmount,
        )
    }

    private fun buildPayableItems(
        expenses: List<Expense>,
        payrollEntries: List<PayrollEntry>,
    ): List<UpcomingPayableItem> {
        val expenseItems = expenses.map { expense ->
            UpcomingPayableItem(
                type = "EXPENSE",
                id = expense.id!!,
                description = expense.supplierName,
                amount = expense.amount.subtract(expense.paidAmount),
                dueDate = expense.dueDate!!,
                supplierName = expense.supplierName,
                category = expense.category.name,
            )
        }
        val payrollItems = payrollEntries.map { entry ->
            UpcomingPayableItem(
                type = "PAYROLL",
                id = entry.id!!,
                description = entry.periodLabel,
                amount = entry.grossAmount,
                dueDate = entry.dueDate,
                employeeName = entry.employee.fullName,
                category = ExpenseCategory.PAYROLL.name,
            )
        }
        return (expenseItems + payrollItems).sortedBy { it.dueDate }
    }
}
