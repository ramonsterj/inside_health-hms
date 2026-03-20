package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.BankStatement
import com.insidehealthgt.hms.entity.BankStatementStatus
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollPeriod
import com.insidehealthgt.hms.entity.PayrollStatus
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.repository.BankAccountRepository
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.DoctorFeeRepository
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.IncomeRepository
import com.insidehealthgt.hms.repository.PayrollEntryRepository
import com.insidehealthgt.hms.repository.TreasuryEmployeeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("LargeClass", "LongMethod")
class TreasuryReportServiceTest {

    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var expensePaymentRepository: ExpensePaymentRepository
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var payrollEntryRepository: PayrollEntryRepository
    private lateinit var employeeRepository: TreasuryEmployeeRepository
    private lateinit var bankStatementRepository: BankStatementRepository
    private lateinit var doctorFeeRepository: DoctorFeeRepository

    private lateinit var service: TreasuryReportService

    @BeforeEach
    fun setUp() {
        bankAccountRepository = mock()
        bankAccountService = mock()
        expenseRepository = mock()
        expensePaymentRepository = mock()
        incomeRepository = mock()
        payrollEntryRepository = mock()
        employeeRepository = mock()
        bankStatementRepository = mock()
        doctorFeeRepository = mock()

        service = TreasuryReportService(
            bankAccountRepository = bankAccountRepository,
            bankAccountService = bankAccountService,
            expenseRepository = expenseRepository,
            expensePaymentRepository = expensePaymentRepository,
            incomeRepository = incomeRepository,
            payrollEntryRepository = payrollEntryRepository,
            employeeRepository = employeeRepository,
            bankStatementRepository = bankStatementRepository,
            doctorFeeRepository = doctorFeeRepository,
        )
    }

    // ─── Helper factories ────────────────────────────────────────────────────────

    private fun makeAccount(
        id: Long = 1L,
        name: String = "Main Account",
        bankName: String = "Banco Industrial",
        accountType: BankAccountType = BankAccountType.CHECKING,
        currency: String = "GTQ",
        openingBalance: BigDecimal = BigDecimal("10000.00"),
        active: Boolean = true,
    ): BankAccount {
        val account = BankAccount(
            name = name,
            bankName = bankName,
            accountType = accountType,
            currency = currency,
            openingBalance = openingBalance,
        )
        account.active = active
        account.id = id
        return account
    }

    private fun makeExpense(
        id: Long = 1L,
        supplierName: String = "Supplier A",
        category: ExpenseCategory = ExpenseCategory.SUPPLIES,
        amount: BigDecimal = BigDecimal("1000.00"),
        paidAmount: BigDecimal = BigDecimal.ZERO,
        expenseDate: LocalDate = LocalDate.now(),
        dueDate: LocalDate? = LocalDate.now().plusDays(5),
        status: ExpenseStatus = ExpenseStatus.PENDING,
        treasuryEmployeeId: Long? = null,
    ): Expense {
        val expense = Expense(
            supplierName = supplierName,
            category = category,
            amount = amount,
            expenseDate = expenseDate,
            invoiceNumber = "INV-$id",
            status = status,
            dueDate = dueDate,
            paidAmount = paidAmount,
            treasuryEmployeeId = treasuryEmployeeId,
        )
        expense.id = id
        return expense
    }

    private fun makeEmployee(
        id: Long = 1L,
        fullName: String = "Juan Perez",
        employeeType: EmployeeType = EmployeeType.PAYROLL,
        baseSalary: BigDecimal? = BigDecimal("5000.00"),
        contractedRate: BigDecimal? = null,
        hireDate: LocalDate? = LocalDate.of(2024, 1, 15),
        position: String? = "Nurse",
    ): TreasuryEmployee {
        val employee = TreasuryEmployee(
            fullName = fullName,
            employeeType = employeeType,
            baseSalary = baseSalary,
            contractedRate = contractedRate,
            hireDate = hireDate,
            position = position,
        )
        employee.id = id
        return employee
    }

    private fun makePayrollEntry(
        id: Long = 1L,
        employee: TreasuryEmployee,
        grossAmount: BigDecimal = BigDecimal("5000.00"),
        dueDate: LocalDate = LocalDate.now().plusDays(3),
        status: PayrollStatus = PayrollStatus.PENDING,
        paidDate: LocalDate? = null,
    ): PayrollEntry {
        val entry = PayrollEntry(
            employee = employee,
            year = dueDate.year,
            period = PayrollPeriod.JANUARY,
            periodLabel = "January 2026",
            baseSalary = employee.baseSalary ?: BigDecimal("5000.00"),
            grossAmount = grossAmount,
            dueDate = dueDate,
            status = status,
            paidDate = paidDate,
        )
        entry.id = id
        return entry
    }

    private fun makeIncome(
        id: Long = 1L,
        description: String = "Patient payment",
        category: IncomeCategory = IncomeCategory.PATIENT_PAYMENT,
        amount: BigDecimal = BigDecimal("2000.00"),
        incomeDate: LocalDate = LocalDate.now(),
        bankAccount: BankAccount = makeAccount(),
        reference: String? = "REF-001",
    ): Income {
        val income = Income(
            description = description,
            category = category,
            amount = amount,
            incomeDate = incomeDate,
            bankAccount = bankAccount,
            reference = reference,
        )
        income.id = id
        return income
    }

    private fun makeExpensePayment(
        id: Long = 1L,
        expense: Expense = makeExpense(),
        amount: BigDecimal = BigDecimal("500.00"),
        paymentDate: LocalDate = LocalDate.now(),
        bankAccount: BankAccount = makeAccount(),
        reference: String? = "PAY-001",
    ): ExpensePayment {
        val payment = ExpensePayment(
            expense = expense,
            amount = amount,
            paymentDate = paymentDate,
            bankAccount = bankAccount,
            reference = reference,
        )
        payment.id = id
        return payment
    }

    private fun makeStatement(
        id: Long = 1L,
        bankAccount: BankAccount = makeAccount(),
        statementDate: LocalDate = LocalDate.now(),
        status: BankStatementStatus = BankStatementStatus.IN_PROGRESS,
        totalRows: Int = 100,
        matchedCount: Int = 60,
        unmatchedCount: Int = 20,
        acknowledgedCount: Int = 15,
    ): BankStatement {
        val stmt = BankStatement(
            bankAccount = bankAccount,
            fileName = "statement-$id.xlsx",
            filePath = "/data/statements/statement-$id.xlsx",
            statementDate = statementDate,
            status = status,
            totalRows = totalRows,
            matchedCount = matchedCount,
            unmatchedCount = unmatchedCount,
            acknowledgedCount = acknowledgedCount,
        )
        stmt.id = id
        return stmt
    }

    // ─── getDashboard ────────────────────────────────────────────────────────────

    @Test
    fun `getDashboard returns bank balances and pending payables`() {
        val account = makeAccount()
        val expense = makeExpense(
            amount = BigDecimal("1000.00"),
            paidAmount = BigDecimal("300.00"),
            status = ExpenseStatus.PARTIALLY_PAID,
        )
        val employee = makeEmployee()
        val payrollEntry = makePayrollEntry(employee = employee, grossAmount = BigDecimal("5000.00"))

        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankAccountService.computeBookBalance(account)).thenReturn(BigDecimal("15000.00"))
        whenever(expenseRepository.findAllByStatusInAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(expense))
        whenever(payrollEntryRepository.findAllByStatusAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(payrollEntry))

        val result = service.getDashboard()

        assertEquals(1, result.bankBalances.size)
        assertEquals(BigDecimal("15000.00"), result.bankBalances[0].bookBalance)
        assertEquals("Main Account", result.bankBalances[0].name)
        assertEquals("GTQ", result.bankBalances[0].currency)
        assertTrue(result.bankBalances[0].active)

        // pending count = 1 expense + 1 payroll = 2
        assertEquals(2, result.pendingPayablesCount)
        // pending total = (1000 - 300) + 5000 = 5700
        assertEquals(BigDecimal("5700.00"), result.pendingPayablesTotal)
    }

    @Test
    fun `getDashboard with empty data returns zeros`() {
        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(emptyList())
        whenever(expenseRepository.findAllByStatusInAndDueDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusAndDueDateBetween(any(), any(), any()))
            .thenReturn(emptyList())

        val result = service.getDashboard()

        assertTrue(result.bankBalances.isEmpty())
        assertEquals(0, result.pendingPayablesCount)
        assertEquals(BigDecimal.ZERO, result.pendingPayablesTotal)
        assertTrue(result.next7DayObligations.isEmpty())
        assertEquals(BigDecimal.ZERO, result.next7DayTotal)
    }

    @Test
    fun `getDashboard returns upcoming obligations sorted by due date`() {
        val expense = makeExpense(
            id = 1L,
            amount = BigDecimal("500.00"),
            dueDate = LocalDate.now().plusDays(3),
        )
        val employee = makeEmployee()
        val payrollEntry = makePayrollEntry(
            id = 2L,
            employee = employee,
            grossAmount = BigDecimal("2000.00"),
            dueDate = LocalDate.now().plusDays(1),
        )

        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(emptyList())
        // 30-day window returns both
        whenever(expenseRepository.findAllByStatusInAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(expense))
        whenever(payrollEntryRepository.findAllByStatusAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(payrollEntry))

        val result = service.getDashboard()

        // The 7-day obligations should have items sorted by dueDate
        assertEquals(2, result.next7DayObligations.size)
        // Payroll entry (dueDate +1) should come before expense (dueDate +3)
        assertEquals("PAYROLL", result.next7DayObligations[0].type)
        assertEquals("EXPENSE", result.next7DayObligations[1].type)
    }

    // ─── getMonthlyReport ────────────────────────────────────────────────────────

    @Test
    fun `getMonthlyReport returns payments and income grouped by category`() {
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 1, 31)
        val account = makeAccount()

        val suppliesExpense1 = makeExpense(id = 1L, category = ExpenseCategory.SUPPLIES)
        val suppliesExpense2 = makeExpense(id = 2L, category = ExpenseCategory.SUPPLIES)
        val utilitiesExpense = makeExpense(id = 3L, category = ExpenseCategory.UTILITIES)

        val payments = listOf(
            makeExpensePayment(
                id = 1L,
                expense = suppliesExpense1,
                amount = BigDecimal("1000.00"),
                paymentDate = from,
                bankAccount = account,
            ),
            makeExpensePayment(
                id = 2L,
                expense = suppliesExpense2,
                amount = BigDecimal("500.00"),
                paymentDate = from,
                bankAccount = account,
            ),
            makeExpensePayment(
                id = 3L,
                expense = utilitiesExpense,
                amount = BigDecimal("800.00"),
                paymentDate = from,
                bankAccount = account,
            ),
        )
        val incomeRecords = listOf(
            makeIncome(
                id = 1L,
                category = IncomeCategory.PATIENT_PAYMENT,
                amount = BigDecimal("3000.00"),
                incomeDate = from,
                bankAccount = account,
            ),
            makeIncome(
                id = 2L,
                category = IncomeCategory.DONATION,
                amount = BigDecimal("500.00"),
                incomeDate = from,
                bankAccount = account,
            ),
        )

        whenever(expensePaymentRepository.findAllByPaymentDateBetween(from, to)).thenReturn(payments)
        whenever(incomeRepository.findAllByIncomeDateBetween(from, to)).thenReturn(incomeRecords)

        val result = service.getMonthlyReport(from, to)

        assertEquals(from, result.from)
        assertEquals(to, result.to)

        // Payments: SUPPLIES=1500, UTILITIES=800, sorted desc by total
        assertEquals(2, result.expensesByCategory.size)
        assertEquals("SUPPLIES", result.expensesByCategory[0].category)
        assertEquals(BigDecimal("1500.00"), result.expensesByCategory[0].total)
        assertEquals(2, result.expensesByCategory[0].count)
        assertEquals("UTILITIES", result.expensesByCategory[1].category)
        assertEquals(BigDecimal("800.00"), result.expensesByCategory[1].total)
        assertEquals(1, result.expensesByCategory[1].count)

        assertEquals(BigDecimal("2300.00"), result.totalExpenses)

        // Income: PATIENT_PAYMENT=3000, DONATION=500
        assertEquals(2, result.incomeByCategory.size)
        assertEquals(BigDecimal("3500.00"), result.totalIncome)

        // Net balance = 3500 - 2300 = 1200
        assertEquals(BigDecimal("1200.00"), result.netBalance)
    }

    @Test
    fun `getMonthlyReport with no data returns zero totals`() {
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 1, 31)

        whenever(expensePaymentRepository.findAllByPaymentDateBetween(from, to)).thenReturn(emptyList())
        whenever(incomeRepository.findAllByIncomeDateBetween(from, to)).thenReturn(emptyList())

        val result = service.getMonthlyReport(from, to)

        assertTrue(result.expensesByCategory.isEmpty())
        assertTrue(result.incomeByCategory.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.netBalance)
    }

    // ─── getUpcomingPayments ─────────────────────────────────────────────────────

    @Test
    fun `getUpcomingPayments returns expenses and payroll sorted by dueDate`() {
        val expense = makeExpense(
            id = 10L,
            supplierName = "Pharma Corp",
            amount = BigDecimal("2000.00"),
            paidAmount = BigDecimal("500.00"),
            status = ExpenseStatus.PARTIALLY_PAID,
            dueDate = LocalDate.now().plusDays(10),
            category = ExpenseCategory.SUPPLIES,
        )
        val employee = makeEmployee(fullName = "Maria Garcia")
        val payroll = makePayrollEntry(
            id = 20L,
            employee = employee,
            grossAmount = BigDecimal("3000.00"),
            dueDate = LocalDate.now().plusDays(5),
        )

        whenever(expenseRepository.findAllByStatusInAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(expense))
        whenever(payrollEntryRepository.findAllByStatusAndDueDateBetween(any(), any(), any()))
            .thenReturn(listOf(payroll))

        val result = service.getUpcomingPayments(30)

        assertEquals(30, result.windowDays)
        assertEquals(2, result.items.size)
        assertEquals(1, result.expenseCount)
        assertEquals(1, result.payrollCount)

        // Payroll dueDate (+5) before Expense dueDate (+10)
        assertEquals("PAYROLL", result.items[0].type)
        assertEquals(BigDecimal("3000.00"), result.items[0].amount)
        assertEquals("Maria Garcia", result.items[0].employeeName)

        assertEquals("EXPENSE", result.items[1].type)
        // Remaining for partially paid: 2000 - 500 = 1500
        assertEquals(BigDecimal("1500.00"), result.items[1].amount)
        assertEquals("Pharma Corp", result.items[1].supplierName)
        assertEquals("SUPPLIES", result.items[1].category)

        // Total: 3000 + 1500 = 4500
        assertEquals(BigDecimal("4500.00"), result.totalAmount)
    }

    @Test
    fun `getUpcomingPayments with no upcoming items returns empty response`() {
        whenever(expenseRepository.findAllByStatusInAndDueDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusAndDueDateBetween(any(), any(), any()))
            .thenReturn(emptyList())

        val result = service.getUpcomingPayments()

        assertTrue(result.items.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        assertEquals(0, result.expenseCount)
        assertEquals(0, result.payrollCount)
    }

    // ─── getBankAccountSummary ───────────────────────────────────────────────────

    @Test
    fun `getBankAccountSummary returns accounts with transactions and statement info`() {
        val account = makeAccount(id = 1L, name = "Checking GTQ", bankName = "Banco Industrial")
        val statement = makeStatement(
            bankAccount = account,
            statementDate = LocalDate.of(2026, 2, 28),
        )

        val expense = makeExpense(supplierName = "Office Depot")
        val payment = makeExpensePayment(
            id = 1L,
            expense = expense,
            amount = BigDecimal("200.00"),
            paymentDate = LocalDate.now().minusDays(1),
            bankAccount = account,
            reference = "PAY-100",
        )

        val incomeItem = makeIncome(
            id = 1L,
            description = "Patient copay",
            amount = BigDecimal("1500.00"),
            incomeDate = LocalDate.now(),
            bankAccount = account,
            reference = "INC-100",
        )

        whenever(bankAccountRepository.findAllByOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankAccountService.computeBookBalance(account)).thenReturn(BigDecimal("12000.00"))
        whenever(bankStatementRepository.findTopByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(statement)
        whenever(expensePaymentRepository.findTop10ByBankAccountIdOrderByPaymentDateDesc(1L))
            .thenReturn(listOf(payment))
        whenever(incomeRepository.findTop10ByBankAccountIdOrderByIncomeDateDesc(1L))
            .thenReturn(listOf(incomeItem))

        val result = service.getBankAccountSummary()

        assertEquals(1, result.accounts.size)
        val item = result.accounts[0]
        assertEquals("Checking GTQ", item.name)
        assertEquals("Banco Industrial", item.bankName)
        assertEquals("CHECKING", item.accountType)
        assertEquals(BigDecimal("12000.00"), item.bookBalance)
        assertEquals(LocalDate.of(2026, 2, 28), item.lastStatementDate)
        assertNull(item.lastStatementBalance)
        assertTrue(item.active)

        // Recent transactions: 1 payment + 1 income = 2, sorted by date desc
        assertEquals(2, item.recentTransactions.size)
        // Income is today, payment is yesterday, so income first
        assertEquals("INCOME", item.recentTransactions[0].type)
        assertEquals(BigDecimal("1500.00"), item.recentTransactions[0].amount)
        assertEquals("EXPENSE_PAYMENT", item.recentTransactions[1].type)
        // Payment amount is negated
        assertEquals(BigDecimal("-200.00"), item.recentTransactions[1].amount)

        assertEquals(BigDecimal("12000.00"), result.totalBookBalance)
    }

    @Test
    fun `getBankAccountSummary with no accounts returns empty list`() {
        whenever(bankAccountRepository.findAllByOrderByNameAsc()).thenReturn(emptyList())

        val result = service.getBankAccountSummary()

        assertTrue(result.accounts.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalBookBalance)
    }

    @Test
    fun `getBankAccountSummary with no statement returns null lastStatementDate`() {
        val account = makeAccount()

        whenever(bankAccountRepository.findAllByOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankAccountService.computeBookBalance(account)).thenReturn(BigDecimal("10000.00"))
        whenever(bankStatementRepository.findTopByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(null)
        whenever(expensePaymentRepository.findTop10ByBankAccountIdOrderByPaymentDateDesc(1L))
            .thenReturn(emptyList())
        whenever(incomeRepository.findTop10ByBankAccountIdOrderByIncomeDateDesc(1L))
            .thenReturn(emptyList())

        val result = service.getBankAccountSummary()

        assertNull(result.accounts[0].lastStatementDate)
        assertTrue(result.accounts[0].recentTransactions.isEmpty())
    }

    // ─── getEmployeeCompensation ─────────────────────────────────────────────────

    @Test
    fun `getEmployeeCompensation with PAYROLL employee returns ytd and pending`() {
        val employee = makeEmployee(
            id = 1L,
            fullName = "Ana Lopez",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("6000.00"),
            position = "Head Nurse",
        )

        val paidEntry = makePayrollEntry(
            id = 10L,
            employee = employee,
            grossAmount = BigDecimal("6000.00"),
            status = PayrollStatus.PAID,
            paidDate = LocalDate.of(2026, 2, 15),
            dueDate = LocalDate.of(2026, 2, 15),
        )
        val pendingEntry = makePayrollEntry(
            id = 11L,
            employee = employee,
            grossAmount = BigDecimal("6000.00"),
            status = PayrollStatus.PENDING,
            dueDate = LocalDate.of(2026, 3, 15),
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(listOf(employee))
        whenever(payrollEntryRepository.findAllByStatusAndPaidDateBetween(eq(PayrollStatus.PAID), any(), any()))
            .thenReturn(listOf(paidEntry))
        whenever(expensePaymentRepository.findAllByPaymentDateBetween(any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusOrderByDueDateAsc(PayrollStatus.PENDING))
            .thenReturn(listOf(pendingEntry))
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusAndFeeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusIn(any()))
            .thenReturn(emptyList())

        val result = service.getEmployeeCompensation(2026)

        assertEquals(2026, result.year)
        assertEquals(1, result.employees.size)
        val comp = result.employees[0]
        assertEquals("Ana Lopez", comp.fullName)
        assertEquals("PAYROLL", comp.employeeType)
        assertEquals("Head Nurse", comp.position)
        assertEquals(BigDecimal("6000.00"), comp.compensation)
        assertEquals(BigDecimal("6000.00"), comp.ytdPayments)
        assertEquals(BigDecimal("6000.00"), comp.pendingAmount)

        assertEquals(BigDecimal("6000.00"), result.totalYtdPayments)
        assertEquals(BigDecimal("6000.00"), result.totalPending)
    }

    @Test
    fun `getEmployeeCompensation with CONTRACTOR employee calculates from expenses`() {
        val employee = makeEmployee(
            id = 2L,
            fullName = "Carlos Ruiz",
            employeeType = EmployeeType.CONTRACTOR,
            baseSalary = null,
            contractedRate = BigDecimal("8000.00"),
        )

        val paidExpense = makeExpense(
            id = 100L,
            category = ExpenseCategory.PAYROLL,
            status = ExpenseStatus.PAID,
            amount = BigDecimal("8000.00"),
            treasuryEmployeeId = 2L,
            expenseDate = LocalDate.of(2026, 2, 1),
        )
        val paidPayment = makeExpensePayment(
            id = 200L,
            expense = paidExpense,
            amount = BigDecimal("8000.00"),
            paymentDate = LocalDate.of(2026, 2, 15),
        )
        val pendingExpense = makeExpense(
            id = 101L,
            category = ExpenseCategory.PAYROLL,
            status = ExpenseStatus.PENDING,
            amount = BigDecimal("8000.00"),
            paidAmount = BigDecimal("2000.00"),
            treasuryEmployeeId = 2L,
            expenseDate = LocalDate.of(2026, 3, 1),
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(listOf(employee))
        // For CONTRACTOR, YTD uses expense payments by paymentDate
        whenever(expensePaymentRepository.findAllByPaymentDateBetween(any(), any()))
            .thenReturn(listOf(paidPayment))
        // For pending, uses Specification-based findAll
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(listOf(pendingExpense))
        // These are needed because the code pre-fetches all data regardless of employee type
        whenever(payrollEntryRepository.findAllByStatusAndPaidDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusOrderByDueDateAsc(PayrollStatus.PENDING))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusAndFeeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusIn(any()))
            .thenReturn(emptyList())

        val result = service.getEmployeeCompensation(2026)

        assertEquals(1, result.employees.size)
        val comp = result.employees[0]
        assertEquals("Carlos Ruiz", comp.fullName)
        assertEquals("CONTRACTOR", comp.employeeType)
        assertEquals(BigDecimal("8000.00"), comp.compensation)
        assertEquals(BigDecimal("8000.00"), comp.ytdPayments)
        // Pending: 8000 - 2000 = 6000
        assertEquals(BigDecimal("6000.00"), comp.pendingAmount)
    }

    @Test
    fun `getEmployeeCompensation with DOCTOR employee returns zero when no fees`() {
        val employee = makeEmployee(
            id = 3L,
            fullName = "Dr. Ramirez",
            employeeType = EmployeeType.DOCTOR,
            baseSalary = null,
            contractedRate = null,
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(listOf(employee))
        whenever(payrollEntryRepository.findAllByStatusAndPaidDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusOrderByDueDateAsc(PayrollStatus.PENDING))
            .thenReturn(emptyList())
        whenever(expensePaymentRepository.findAllByPaymentDateBetween(any(), any()))
            .thenReturn(emptyList())
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusAndFeeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusIn(any()))
            .thenReturn(emptyList())

        val result = service.getEmployeeCompensation(2026)

        assertEquals(1, result.employees.size)
        val comp = result.employees[0]
        assertEquals("Dr. Ramirez", comp.fullName)
        assertEquals("DOCTOR", comp.employeeType)
        assertNull(comp.compensation)
        assertEquals(BigDecimal.ZERO, comp.ytdPayments)
        assertEquals(BigDecimal.ZERO, comp.pendingAmount)
    }

    @Test
    fun `getEmployeeCompensation with no employees returns empty response`() {
        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusAndPaidDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(payrollEntryRepository.findAllByStatusOrderByDueDateAsc(PayrollStatus.PENDING))
            .thenReturn(emptyList())
        whenever(expensePaymentRepository.findAllByPaymentDateBetween(any(), any()))
            .thenReturn(emptyList())
        whenever(expenseRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Expense>>()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusAndFeeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(doctorFeeRepository.findAllByStatusIn(any()))
            .thenReturn(emptyList())

        val result = service.getEmployeeCompensation(2026)

        assertTrue(result.employees.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalYtdPayments)
        assertEquals(BigDecimal.ZERO, result.totalPending)
    }

    // ─── getIndemnizacionLiability ───────────────────────────────────────────────

    @Test
    fun `getIndemnizacionLiability calculates liability for eligible PAYROLL employees`() {
        val employee = makeEmployee(
            id = 1L,
            fullName = "Pedro Gomez",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("5000.00"),
            hireDate = LocalDate.now().minusDays(365),
            position = "Orderly",
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(listOf(employee))

        val result = service.getIndemnizacionLiability()

        assertEquals(LocalDate.now(), result.asOfDate)
        assertEquals(1, result.employees.size)
        val item = result.employees[0]
        assertEquals("Pedro Gomez", item.fullName)
        assertEquals("Orderly", item.position)
        assertEquals(365L, item.tenureDays)
        assertEquals(BigDecimal("5000.00"), item.currentSalary)
        // Liability: 5000 * (365 / 365) = 5000.00
        assertEquals(BigDecimal("5000.00"), item.liability)
        assertEquals(BigDecimal("5000.00"), result.grandTotal)
    }

    @Test
    fun `getIndemnizacionLiability filters out employees without hireDate`() {
        val withHireDate = makeEmployee(
            id = 1L,
            fullName = "With Date",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("5000.00"),
            hireDate = LocalDate.now().minusDays(100),
        )
        val withoutHireDate = makeEmployee(
            id = 2L,
            fullName = "No Date",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("5000.00"),
            hireDate = null,
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc())
            .thenReturn(listOf(withHireDate, withoutHireDate))

        val result = service.getIndemnizacionLiability()

        assertEquals(1, result.employees.size)
        assertEquals("With Date", result.employees[0].fullName)
    }

    @Test
    fun `getIndemnizacionLiability filters out employees without baseSalary`() {
        val withSalary = makeEmployee(
            id = 1L,
            fullName = "Has Salary",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = BigDecimal("4000.00"),
            hireDate = LocalDate.now().minusDays(200),
        )
        val withoutSalary = makeEmployee(
            id = 2L,
            fullName = "No Salary",
            employeeType = EmployeeType.PAYROLL,
            baseSalary = null,
            hireDate = LocalDate.now().minusDays(200),
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc())
            .thenReturn(listOf(withSalary, withoutSalary))

        val result = service.getIndemnizacionLiability()

        assertEquals(1, result.employees.size)
        assertEquals("Has Salary", result.employees[0].fullName)
    }

    @Test
    fun `getIndemnizacionLiability filters out CONTRACTOR employees`() {
        val contractor = makeEmployee(
            id = 1L,
            fullName = "Contractor Joe",
            employeeType = EmployeeType.CONTRACTOR,
            baseSalary = BigDecimal("5000.00"),
            hireDate = LocalDate.now().minusDays(365),
        )

        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(listOf(contractor))

        val result = service.getIndemnizacionLiability()

        assertTrue(result.employees.isEmpty())
        assertEquals(BigDecimal.ZERO, result.grandTotal)
    }

    @Test
    fun `getIndemnizacionLiability with no eligible employees returns empty`() {
        whenever(employeeRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(emptyList())

        val result = service.getIndemnizacionLiability()

        assertTrue(result.employees.isEmpty())
        assertEquals(BigDecimal.ZERO, result.grandTotal)
    }

    // ─── getReconciliationSummary ────────────────────────────────────────────────

    @Test
    fun `getReconciliationSummary returns summary per active account`() {
        val account = makeAccount(id = 1L, name = "Main Checking")
        val completedStatement = makeStatement(
            id = 1L,
            bankAccount = account,
            statementDate = LocalDate.of(2026, 1, 31),
            status = BankStatementStatus.COMPLETED,
            totalRows = 50,
            matchedCount = 40,
            unmatchedCount = 5,
            acknowledgedCount = 5,
        )
        val inProgressStatement = makeStatement(
            id = 2L,
            bankAccount = account,
            statementDate = LocalDate.of(2026, 2, 28),
            status = BankStatementStatus.IN_PROGRESS,
            totalRows = 30,
            matchedCount = 10,
            unmatchedCount = 15,
            acknowledgedCount = 3,
        )

        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(listOf(account))
        // Returned desc by date, so Feb first
        whenever(bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(listOf(inProgressStatement, completedStatement))

        val result = service.getReconciliationSummary()

        assertEquals(1, result.accounts.size)
        val item = result.accounts[0]
        assertEquals(1L, item.bankAccountId)
        assertEquals("Main Checking", item.bankAccountName)
        assertEquals(2, item.totalStatements)

        // Totals across both statements
        assertEquals(80, item.totalRows) // 50 + 30
        assertEquals(50, item.matchedCount) // 40 + 10
        assertEquals(20, item.unmatchedCount) // 5 + 15
        assertEquals(8, item.acknowledgedCount) // 5 + 3

        // Coverage: (50 + 8) * 100 / 80 = 72.50
        assertEquals(BigDecimal("72.50"), item.coveragePct)

        // Last completed statement date
        assertEquals(LocalDate.of(2026, 1, 31), item.lastReconciliationDate)
        // First statement (most recent) status
        assertEquals("IN_PROGRESS", item.lastStatementStatus)
    }

    @Test
    fun `getReconciliationSummary handles division by zero when totalRows is 0`() {
        val account = makeAccount(id = 1L, name = "Empty Account")
        val statement = makeStatement(
            id = 1L,
            bankAccount = account,
            totalRows = 0,
            matchedCount = 0,
            unmatchedCount = 0,
            acknowledgedCount = 0,
        )

        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(listOf(statement))

        val result = service.getReconciliationSummary()

        assertEquals(1, result.accounts.size)
        assertEquals(BigDecimal.ZERO, result.accounts[0].coveragePct)
    }

    @Test
    fun `getReconciliationSummary with no statements returns zero values`() {
        val account = makeAccount(id = 1L, name = "New Account")

        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(emptyList())

        val result = service.getReconciliationSummary()

        assertEquals(1, result.accounts.size)
        val item = result.accounts[0]
        assertEquals(0, item.totalStatements)
        assertEquals(0, item.totalRows)
        assertEquals(0, item.matchedCount)
        assertEquals(0, item.unmatchedCount)
        assertEquals(0, item.acknowledgedCount)
        assertEquals(BigDecimal.ZERO, item.coveragePct)
        assertNull(item.lastReconciliationDate)
        assertNull(item.lastStatementStatus)
    }

    @Test
    fun `getReconciliationSummary with no active accounts returns empty`() {
        whenever(bankAccountRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(emptyList())

        val result = service.getReconciliationSummary()

        assertTrue(result.accounts.isEmpty())
    }
}
