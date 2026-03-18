package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateIncomeRequest
import com.insidehealthgt.hms.dto.request.UpdateIncomeRequest
import com.insidehealthgt.hms.dto.response.IncomeResponse
import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.IncomeRepository
import com.insidehealthgt.hms.repository.IncomeSpecification
import com.insidehealthgt.hms.repository.InvoiceRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class IncomeService(
    private val incomeRepository: IncomeRepository,
    private val bankAccountService: BankAccountService,
    private val invoiceRepository: InvoiceRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(
        category: IncomeCategory?,
        bankAccountId: Long?,
        from: LocalDate?,
        to: LocalDate?,
        search: String?,
        pageable: Pageable,
    ): Page<IncomeResponse> {
        val spec = IncomeSpecification.withFilters(
            category,
            bankAccountId,
            from,
            to,
            search?.takeIf { it.isNotBlank() },
        )
        val effectivePageable = if (pageable.sort.isUnsorted) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "incomeDate", "id"))
        } else {
            pageable
        }
        val page = incomeRepository.findAll(spec, effectivePageable)
        val userIds = page.content.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.toSet()
        val usersById = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        }
        val invoiceIds = page.content.mapNotNull { it.invoiceId }.toSet()
        val invoiceNumbersById = resolveInvoiceNumbers(invoiceIds)
        return page.map { buildResponse(it, usersById, invoiceNumbersById) }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): IncomeResponse {
        val income = findEntityById(id)
        return buildResponse(income)
    }

    @Transactional
    fun create(request: CreateIncomeRequest): IncomeResponse {
        val bankAccount = bankAccountService.findEntityById(request.bankAccountId)
        if (request.invoiceId != null && !invoiceRepository.existsById(request.invoiceId)) {
            throw BadRequestException("Invoice not found with id: ${request.invoiceId}")
        }
        val income = Income(
            description = request.description,
            category = request.category,
            amount = request.amount,
            incomeDate = request.incomeDate,
            reference = request.reference?.takeIf { it.isNotBlank() },
            bankAccount = bankAccount,
            invoiceId = request.invoiceId,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val saved = incomeRepository.save(income)
        return buildResponse(saved)
    }

    @Transactional
    fun update(id: Long, request: UpdateIncomeRequest): IncomeResponse {
        val income = findEntityById(id)
        val bankAccount = bankAccountService.findEntityById(request.bankAccountId)
        if (request.invoiceId != null && !invoiceRepository.existsById(request.invoiceId)) {
            throw BadRequestException("Invoice not found with id: ${request.invoiceId}")
        }
        income.description = request.description
        income.category = request.category
        income.amount = request.amount
        income.incomeDate = request.incomeDate
        income.reference = request.reference?.takeIf { it.isNotBlank() }
        income.bankAccount = bankAccount
        income.invoiceId = request.invoiceId
        income.notes = request.notes?.takeIf { it.isNotBlank() }
        val saved = incomeRepository.save(income)
        return buildResponse(saved)
    }

    @Transactional
    fun delete(id: Long) {
        val income = findEntityById(id)
        income.deletedAt = LocalDateTime.now()
        incomeRepository.save(income)
    }

    fun findEntityById(id: Long): Income = incomeRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Income record not found with id: $id") }

    private fun buildResponse(
        income: Income,
        usersById: Map<Long, User> = emptyMap(),
        invoiceNumbersById: Map<Long, String> = emptyMap(),
    ): IncomeResponse {
        val createdByUser = income.createdBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        val updatedByUser = income.updatedBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        val invoiceNumber = income.invoiceId?.let {
            invoiceNumbersById[it] ?: invoiceRepository.findById(it).orElse(null)?.invoiceNumber
        }
        return IncomeResponse.from(income, invoiceNumber, createdByUser, updatedByUser)
    }

    private fun resolveInvoiceNumbers(invoiceIds: Set<Long>): Map<Long, String> {
        if (invoiceIds.isEmpty()) return emptyMap()
        return invoiceRepository.findInvoiceNumbersByIds(invoiceIds)
            .associate { (it[0] as Long) to (it[1] as String) }
    }
}
