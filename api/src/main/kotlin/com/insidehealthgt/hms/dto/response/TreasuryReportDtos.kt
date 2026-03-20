package com.insidehealthgt.hms.dto.response

import java.math.BigDecimal
import java.time.LocalDate

data class TreasuryDashboardResponse(
    val bankBalances: List<BankBalanceSnapshot>,
    val pendingPayablesCount: Int,
    val pendingPayablesTotal: BigDecimal,
    val next7DayObligations: List<UpcomingPayableItem>,
    val next7DayTotal: BigDecimal,
)

data class BankBalanceSnapshot(
    val bankAccountId: Long,
    val name: String,
    val bookBalance: BigDecimal,
    val currency: String,
    val active: Boolean,
)

data class MonthlyPaymentReportResponse(
    val from: LocalDate,
    val to: LocalDate,
    val expensesByCategory: List<MonthlyCategoryBreakdown>,
    val totalExpenses: BigDecimal,
    val incomeByCategory: List<MonthlyCategoryBreakdown>,
    val totalIncome: BigDecimal,
    val netBalance: BigDecimal,
)

data class MonthlyCategoryBreakdown(val category: String, val label: String, val total: BigDecimal, val count: Int)

data class UpcomingPaymentsResponse(
    val windowDays: Int,
    val from: LocalDate,
    val to: LocalDate,
    val items: List<UpcomingPayableItem>,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
    val payrollCount: Int,
)

data class UpcomingPayableItem(
    val type: String,
    val id: Long,
    val description: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
    val employeeName: String? = null,
    val supplierName: String? = null,
    val category: String? = null,
)

data class BankAccountSummaryResponse(val accounts: List<BankAccountSummaryItem>, val totalBookBalance: BigDecimal)

data class BankAccountSummaryItem(
    val bankAccountId: Long,
    val name: String,
    val bankName: String?,
    val accountType: String,
    val currency: String,
    val openingBalance: BigDecimal,
    val bookBalance: BigDecimal,
    val lastStatementDate: LocalDate?,
    val lastStatementBalance: BigDecimal?,
    val active: Boolean,
    val recentTransactions: List<RecentTransactionItem>,
)

data class RecentTransactionItem(
    val type: String,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val reference: String?,
)

data class EmployeeCompensationResponse(
    val year: Int,
    val employees: List<EmployeeCompensationItem>,
    val totalYtdPayments: BigDecimal,
    val totalPending: BigDecimal,
)

data class EmployeeCompensationItem(
    val employeeId: Long,
    val fullName: String,
    val employeeType: String,
    val position: String?,
    val compensation: BigDecimal?,
    val ytdPayments: BigDecimal,
    val pendingAmount: BigDecimal,
)

data class IndemnizacionLiabilityResponse(
    val asOfDate: LocalDate,
    val employees: List<IndemnizacionLiabilityItem>,
    val grandTotal: BigDecimal,
)

data class IndemnizacionLiabilityItem(
    val employeeId: Long,
    val fullName: String,
    val position: String?,
    val hireDate: LocalDate,
    val tenureDays: Long,
    val currentSalary: BigDecimal,
    val liability: BigDecimal,
)

data class ReconciliationSummaryResponse(val accounts: List<ReconciliationSummaryItem>)

data class ReconciliationSummaryItem(
    val bankAccountId: Long,
    val bankAccountName: String,
    val lastReconciliationDate: LocalDate?,
    val lastStatementStatus: String?,
    val totalStatements: Int,
    val totalRows: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val acknowledgedCount: Int,
    val coveragePct: BigDecimal,
)
