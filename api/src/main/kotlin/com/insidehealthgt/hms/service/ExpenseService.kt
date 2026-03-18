package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateExpenseRequest
import com.insidehealthgt.hms.dto.request.RecordExpensePaymentRequest
import com.insidehealthgt.hms.dto.request.UpdateExpenseRequest
import com.insidehealthgt.hms.dto.response.ExpensePaymentResponse
import com.insidehealthgt.hms.dto.response.ExpenseResponse
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.ExpenseSpecification
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CreatePaidExpenseCommand(
    val supplierName: String,
    val category: ExpenseCategory,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val invoiceNumber: String,
    val bankAccountId: Long,
    val paymentDate: LocalDate,
    val paymentReference: String? = null,
    val invoiceDocumentPath: String? = null,
    val treasuryEmployeeId: Long? = null,
    val notes: String? = null,
)

@Suppress("TooManyFunctions")
@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val bankAccountService: BankAccountService,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(
        status: ExpenseStatus?,
        category: ExpenseCategory?,
        from: LocalDate?,
        to: LocalDate?,
        search: String?,
        pageable: Pageable,
    ): Page<ExpenseResponse> {
        val spec = ExpenseSpecification.withFilters(status, category, from, to, search?.takeIf { it.isNotBlank() })
        val effectivePageable = if (pageable.sort.isUnsorted) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "expenseDate", "id"))
        } else {
            pageable
        }
        val page = expenseRepository.findAll(spec, effectivePageable)
        val userIds = page.content.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.toSet()
        val usersById = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        }
        return page.map { buildResponse(it, usersById) }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): ExpenseResponse {
        val expense = findEntityById(id)
        return buildResponse(expense)
    }

    @Transactional
    fun create(request: CreateExpenseRequest, invoiceFile: MultipartFile? = null): ExpenseResponse {
        validateCreateRequest(request)

        val expense = Expense(
            supplierName = request.supplierName,
            category = request.category,
            description = request.description?.takeIf { it.isNotBlank() },
            amount = request.amount,
            expenseDate = request.expenseDate,
            invoiceNumber = request.invoiceNumber,
            status = if (request.isPaid) ExpenseStatus.PAID else ExpenseStatus.PENDING,
            dueDate = if (!request.isPaid) request.dueDate else null,
            paidAmount = if (request.isPaid) request.amount else BigDecimal.ZERO,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val saved = expenseRepository.save(expense)

        // Store invoice file if provided
        if (invoiceFile != null && !invoiceFile.isEmpty) {
            val path = fileStorageService.storeExpenseInvoice(saved.id!!, invoiceFile)
            saved.invoiceDocumentPath = path
            expenseRepository.save(saved)
        }

        // Create initial payment record if already paid
        if (request.isPaid) {
            val bankAccount = bankAccountService.findEntityById(request.bankAccountId!!)
            val payment = ExpensePayment(
                expense = saved,
                amount = request.amount,
                paymentDate = request.paymentDate!!,
                bankAccount = bankAccount,
            )
            expensePaymentRepository.save(payment)
        }

        return buildResponse(saved)
    }

    @Transactional
    fun update(id: Long, request: UpdateExpenseRequest): ExpenseResponse {
        val expense = findEntityById(id)
        if (expense.status == ExpenseStatus.CANCELLED) {
            throw BadRequestException("Cannot update a cancelled expense")
        }
        if (request.amount < expense.paidAmount) {
            throw BadRequestException("Cannot reduce amount below already paid amount (${expense.paidAmount})")
        }
        expense.supplierName = request.supplierName
        expense.category = request.category
        expense.description = request.description?.takeIf { it.isNotBlank() }
        expense.amount = request.amount
        if (expense.paidAmount > BigDecimal.ZERO) {
            expense.status = if (expense.paidAmount >= expense.amount) {
                ExpenseStatus.PAID
            } else {
                ExpenseStatus.PARTIALLY_PAID
            }
        }
        expense.expenseDate = request.expenseDate
        expense.invoiceNumber = request.invoiceNumber
        expense.dueDate = request.dueDate
        expense.notes = request.notes?.takeIf { it.isNotBlank() }
        val saved = expenseRepository.save(expense)
        return buildResponse(saved)
    }

    @Transactional
    fun delete(id: Long) {
        val expense = findEntityById(id)
        if (expense.paidAmount > BigDecimal.ZERO) {
            throw BadRequestException("Cannot delete an expense with recorded payments")
        }
        expense.deletedAt = LocalDateTime.now()
        expenseRepository.save(expense)
    }

    @Transactional
    fun uploadInvoiceDocument(id: Long, file: MultipartFile): ExpenseResponse {
        val expense = findEntityById(id)
        val path = fileStorageService.storeExpenseInvoice(id, file)
        expense.invoiceDocumentPath = path
        val saved = expenseRepository.save(expense)
        return buildResponse(saved)
    }

    @Transactional
    fun recordPayment(id: Long, request: RecordExpensePaymentRequest): ExpensePaymentResponse {
        val expense = findEntityById(id)
        validateExpensePayable(expense)

        val remaining = expense.amount.subtract(expense.paidAmount)
        if (request.amount > remaining) {
            throw BadRequestException("Payment amount (${request.amount}) exceeds remaining balance ($remaining)")
        }

        val bankAccount = bankAccountService.findEntityById(request.bankAccountId)
        val payment = ExpensePayment(
            expense = expense,
            amount = request.amount,
            paymentDate = request.paymentDate,
            bankAccount = bankAccount,
            reference = request.reference?.takeIf { it.isNotBlank() },
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val savedPayment = expensePaymentRepository.save(payment)

        // Update paid amount and status
        expense.paidAmount = expense.paidAmount.add(request.amount)
        expense.status = if (expense.paidAmount >= expense.amount) {
            ExpenseStatus.PAID
        } else {
            ExpenseStatus.PARTIALLY_PAID
        }
        expenseRepository.save(expense)

        return ExpensePaymentResponse.from(savedPayment)
    }

    @Transactional(readOnly = true)
    fun getPayments(id: Long): List<ExpensePaymentResponse> {
        findEntityById(id) // verify expense exists
        return expensePaymentRepository.findAllByExpenseIdOrderByPaymentDateAsc(id)
            .map { ExpensePaymentResponse.from(it) }
    }

    @Transactional
    fun createPaidExpense(command: CreatePaidExpenseCommand): Expense {
        val bankAccount = bankAccountService.findEntityById(command.bankAccountId)
        val expense = Expense(
            supplierName = command.supplierName,
            category = command.category,
            amount = command.amount,
            expenseDate = command.expenseDate,
            invoiceNumber = command.invoiceNumber,
            invoiceDocumentPath = command.invoiceDocumentPath,
            status = ExpenseStatus.PAID,
            paidAmount = command.amount,
            treasuryEmployeeId = command.treasuryEmployeeId,
            notes = command.notes?.takeIf { it.isNotBlank() },
        )
        val savedExpense = expenseRepository.save(expense)
        val payment = ExpensePayment(
            expense = savedExpense,
            amount = command.amount,
            paymentDate = command.paymentDate,
            bankAccount = bankAccount,
            reference = command.paymentReference,
        )
        expensePaymentRepository.save(payment)
        return savedExpense
    }

    fun findEntityById(id: Long): Expense = expenseRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Expense not found with id: $id") }

    private fun validateCreateRequest(request: CreateExpenseRequest) {
        if (!request.isPaid && request.dueDate == null) {
            throw BadRequestException("Due date is required when expense is not paid")
        }
        validatePaidExpenseFields(request)
    }

    private fun validatePaidExpenseFields(request: CreateExpenseRequest) {
        if (request.isPaid && request.paymentDate == null) {
            throw BadRequestException("Payment date is required when expense is paid")
        }
        if (request.isPaid && request.bankAccountId == null) {
            throw BadRequestException("Bank account is required when expense is paid")
        }
    }

    private fun validateExpensePayable(expense: Expense) {
        if (expense.status == ExpenseStatus.PAID) {
            throw BadRequestException("Expense is already fully paid")
        }
        if (expense.status == ExpenseStatus.CANCELLED) {
            throw BadRequestException("Cannot record payment for a cancelled expense")
        }
    }

    private fun buildResponse(expense: Expense, usersById: Map<Long, User> = emptyMap()): ExpenseResponse {
        val createdByUser = expense.createdBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        val updatedByUser = expense.updatedBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        return ExpenseResponse.from(expense, createdByUser, updatedByUser)
    }
}
