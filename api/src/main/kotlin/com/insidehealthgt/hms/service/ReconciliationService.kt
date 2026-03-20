package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.AcknowledgeRowRequest
import com.insidehealthgt.hms.dto.request.CreateExpenseFromRowRequest
import com.insidehealthgt.hms.dto.request.CreateIncomeFromRowRequest
import com.insidehealthgt.hms.dto.request.CreateIncomeRequest
import com.insidehealthgt.hms.dto.request.MatchRowRequest
import com.insidehealthgt.hms.dto.response.BankStatementRowResponse
import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.BankStatementStatus
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.MatchedEntityType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.BankStatementRowRepository
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.IncomeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

@Suppress("TooManyFunctions")
@Service
class ReconciliationService(
    private val bankStatementRepository: BankStatementRepository,
    private val bankStatementRowRepository: BankStatementRowRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseService: ExpenseService,
    private val incomeService: IncomeService,
    private val bankAccountService: BankAccountService,
) {

    private val logger = LoggerFactory.getLogger(ReconciliationService::class.java)

    companion object {
        private const val DATE_TOLERANCE_DAYS = 3L
    }

    @Transactional
    fun autoMatch(statementId: Long) {
        val statement = bankStatementRepository.findById(statementId)
            .orElseThrow { ResourceNotFoundException("Bank statement not found with id: $statementId") }
        val bankAccountId = statement.bankAccount.id!!
        val rows = bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(statementId)

        val unmatchedRows = rows.filter { it.matchStatus == MatchStatus.UNMATCHED }
        if (unmatchedRows.isEmpty()) return

        val minDate = unmatchedRows.minOf { it.transactionDate }.minusDays(DATE_TOLERANCE_DAYS)
        val maxDate = unmatchedRows.maxOf { it.transactionDate }.plusDays(DATE_TOLERANCE_DAYS)

        val expensePayments = expensePaymentRepository.findAllByBankAccountIdAndPaymentDateBetween(
            bankAccountId,
            minDate,
            maxDate,
        )
        val incomeRecords = incomeRepository.findAllByBankAccountIdAndIncomeDateBetween(
            bankAccountId,
            minDate,
            maxDate,
        )

        val matchedEntityIds = mutableSetOf<String>()

        for (row in unmatchedRows) {
            if (row.debitAmount != null && row.debitAmount!!.compareTo(BigDecimal.ZERO) != 0) {
                matchDebitRow(row, expensePayments, matchedEntityIds)
            } else if (row.creditAmount != null && row.creditAmount!!.compareTo(BigDecimal.ZERO) != 0) {
                matchCreditRow(row, incomeRecords, matchedEntityIds)
            }
        }

        updateStatementCounters(statementId)
    }

    @Transactional
    fun confirmMatch(bankAccountId: Long, statementId: Long, rowId: Long): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.SUGGESTED) {
            throw BadRequestException("Can only confirm rows in SUGGESTED status")
        }
        validateEntityNotAlreadyMatched(row.matchedEntityType!!, row.matchedEntityId!!, rowId)
        row.matchStatus = MatchStatus.MATCHED
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved, resolveMatchedEntityDescription(saved))
    }

    @Transactional
    fun rejectMatch(bankAccountId: Long, statementId: Long, rowId: Long): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.SUGGESTED) {
            throw BadRequestException("Can only reject rows in SUGGESTED status")
        }
        row.matchStatus = MatchStatus.UNMATCHED
        row.matchedEntityType = null
        row.matchedEntityId = null
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved)
    }

    @Transactional
    fun manualMatch(
        bankAccountId: Long,
        statementId: Long,
        rowId: Long,
        request: MatchRowRequest,
    ): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.UNMATCHED) {
            throw BadRequestException("Can only manually match rows in UNMATCHED status")
        }
        validateEntityExists(request.matchedEntityType, request.matchedEntityId, bankAccountId)
        validateEntityNotAlreadyMatched(request.matchedEntityType, request.matchedEntityId, rowId)
        row.matchStatus = MatchStatus.MATCHED
        row.matchedEntityType = request.matchedEntityType
        row.matchedEntityId = request.matchedEntityId
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved, resolveMatchedEntityDescription(saved))
    }

    @Transactional
    fun acknowledgeRow(
        bankAccountId: Long,
        statementId: Long,
        rowId: Long,
        request: AcknowledgeRowRequest,
    ): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.UNMATCHED) {
            throw BadRequestException("Can only acknowledge rows in UNMATCHED status")
        }
        row.matchStatus = MatchStatus.ACKNOWLEDGED
        row.acknowledgedReason = request.reason
        row.nonLedger = request.nonLedger
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved)
    }

    @Transactional
    fun createExpenseAndMatch(
        bankAccountId: Long,
        statementId: Long,
        rowId: Long,
        request: CreateExpenseFromRowRequest,
    ): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.UNMATCHED) {
            throw BadRequestException("Can only create expense for rows in UNMATCHED status")
        }

        val expense = expenseService.createPaidExpense(
            CreatePaidExpenseCommand(
                supplierName = request.supplierName,
                category = request.category,
                amount = request.amount,
                expenseDate = request.expenseDate,
                invoiceNumber = request.invoiceNumber,
                bankAccountId = bankAccountId,
                paymentDate = request.expenseDate,
                paymentReference = "STMT-ROW-${row.rowNumber}",
                notes = request.notes,
            ),
        )

        val payments = expensePaymentRepository.findAllByExpenseIdOrderByPaymentDateAsc(expense.id!!)
        val payment = payments.first()

        row.matchStatus = MatchStatus.MATCHED
        row.matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT
        row.matchedEntityId = payment.id
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved, resolveMatchedEntityDescription(saved))
    }

    @Transactional
    fun createIncomeAndMatch(
        bankAccountId: Long,
        statementId: Long,
        rowId: Long,
        request: CreateIncomeFromRowRequest,
    ): BankStatementRowResponse {
        val row = findRow(bankAccountId, statementId, rowId)
        if (row.matchStatus != MatchStatus.UNMATCHED) {
            throw BadRequestException("Can only create income for rows in UNMATCHED status")
        }

        val incomeResponse = incomeService.create(
            CreateIncomeRequest(
                description = request.description,
                category = request.category,
                amount = request.amount,
                incomeDate = request.incomeDate,
                reference = request.reference,
                bankAccountId = bankAccountId,
                notes = request.notes,
            ),
        )

        row.matchStatus = MatchStatus.MATCHED
        row.matchedEntityType = MatchedEntityType.INCOME
        row.matchedEntityId = incomeResponse.id
        val saved = bankStatementRowRepository.save(row)
        updateStatementCounters(statementId)
        return BankStatementRowResponse.from(saved, resolveMatchedEntityDescription(saved))
    }

    fun resolveMatchedEntityDescription(row: BankStatementRow): String? {
        if (row.matchedEntityType == null || row.matchedEntityId == null) return null
        return when (row.matchedEntityType!!) {
            MatchedEntityType.EXPENSE_PAYMENT -> {
                val payment = expensePaymentRepository.findById(row.matchedEntityId!!).orElse(null)
                if (payment != null) {
                    "${payment.expense.supplierName} - ${payment.expense.invoiceNumber}"
                } else {
                    null
                }
            }

            MatchedEntityType.INCOME -> {
                val income = incomeRepository.findById(row.matchedEntityId!!).orElse(null)
                income?.description
            }
        }
    }

    private fun <T> matchRow(
        row: BankStatementRow,
        candidates: List<T>,
        entityType: MatchedEntityType,
        rowAmount: BigDecimal?,
        amountOf: (T) -> BigDecimal,
        dateOf: (T) -> LocalDate,
        idOf: (T) -> Long?,
        matchedEntityIds: MutableSet<String>,
    ) {
        val matches = candidates.filter { candidate ->
            amountOf(candidate).compareTo(rowAmount) == 0 &&
                isWithinDateTolerance(row.transactionDate, dateOf(candidate)) &&
                "$entityType:${idOf(candidate)}" !in matchedEntityIds
        }

        if (matches.size == 1) {
            val matchId = idOf(matches[0])
            matchedEntityIds.add("$entityType:$matchId")
            row.matchStatus = MatchStatus.SUGGESTED
            row.matchedEntityType = entityType
            row.matchedEntityId = matchId
            bankStatementRowRepository.save(row)
        }
    }

    private fun matchDebitRow(
        row: BankStatementRow,
        payments: List<ExpensePayment>,
        matchedEntityIds: MutableSet<String>,
    ) = matchRow(
        row = row,
        candidates = payments,
        entityType = MatchedEntityType.EXPENSE_PAYMENT,
        rowAmount = row.debitAmount,
        amountOf = { it.amount },
        dateOf = { it.paymentDate },
        idOf = { it.id },
        matchedEntityIds = matchedEntityIds,
    )

    private fun matchCreditRow(
        row: BankStatementRow,
        incomeRecords: List<Income>,
        matchedEntityIds: MutableSet<String>,
    ) = matchRow(
        row = row,
        candidates = incomeRecords,
        entityType = MatchedEntityType.INCOME,
        rowAmount = row.creditAmount,
        amountOf = { it.amount },
        dateOf = { it.incomeDate },
        idOf = { it.id },
        matchedEntityIds = matchedEntityIds,
    )

    private fun isWithinDateTolerance(statementDate: LocalDate, recordDate: LocalDate): Boolean {
        val diff = abs(statementDate.toEpochDay() - recordDate.toEpochDay())
        return diff <= DATE_TOLERANCE_DAYS
    }

    private fun findRow(bankAccountId: Long, statementId: Long, rowId: Long): BankStatementRow {
        val row = bankStatementRowRepository.findById(rowId)
            .orElseThrow { ResourceNotFoundException("Statement row not found with id: $rowId") }
        if (row.bankStatement.id != statementId) {
            throw ResourceNotFoundException("Statement row not found with id: $rowId")
        }
        if (row.bankStatement.bankAccount.id != bankAccountId) {
            throw ResourceNotFoundException("Bank statement not found with id: $statementId")
        }
        return row
    }

    private fun validateEntityNotAlreadyMatched(entityType: MatchedEntityType, entityId: Long, excludeRowId: Long) {
        val alreadyMatched = bankStatementRowRepository
            .existsByMatchedEntityTypeAndMatchedEntityIdAndMatchStatusAndIdNot(
                entityType,
                entityId,
                MatchStatus.MATCHED,
                excludeRowId,
            )
        if (alreadyMatched) {
            throw BadRequestException("This entity is already matched to another row")
        }
    }

    private fun validateEntityExists(entityType: MatchedEntityType, entityId: Long, bankAccountId: Long) {
        when (entityType) {
            MatchedEntityType.EXPENSE_PAYMENT -> {
                val payment = expensePaymentRepository.findById(entityId)
                    .orElseThrow { ResourceNotFoundException("Expense payment not found with id: $entityId") }
                if (payment.bankAccount.id != bankAccountId) {
                    throw BadRequestException("Expense payment does not belong to this bank account")
                }
            }

            MatchedEntityType.INCOME -> {
                val income = incomeRepository.findById(entityId)
                    .orElseThrow { ResourceNotFoundException("Income record not found with id: $entityId") }
                if (income.bankAccount.id != bankAccountId) {
                    throw BadRequestException("Income record does not belong to this bank account")
                }
            }
        }
    }

    private fun updateStatementCounters(statementId: Long) {
        val statement = bankStatementRepository.findById(statementId)
            .orElseThrow { ResourceNotFoundException("Bank statement not found with id: $statementId") }
        val counts = bankStatementRowRepository.countGroupedByMatchStatus(statementId)
        val countMap = counts.associate { (it[0] as MatchStatus) to (it[1] as Long).toInt() }
        statement.matchedCount = countMap[MatchStatus.MATCHED] ?: 0
        statement.unmatchedCount = countMap[MatchStatus.UNMATCHED] ?: 0
        statement.suggestedCount = countMap[MatchStatus.SUGGESTED] ?: 0
        statement.acknowledgedCount = countMap[MatchStatus.ACKNOWLEDGED] ?: 0
        if (statement.status == BankStatementStatus.PENDING) {
            statement.status = BankStatementStatus.IN_PROGRESS
        }
        bankStatementRepository.save(statement)
    }
}
