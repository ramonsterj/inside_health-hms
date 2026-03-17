package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateExpenseRequest
import com.insidehealthgt.hms.dto.request.RecordExpensePaymentRequest
import com.insidehealthgt.hms.dto.request.UpdateExpenseRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.ExpensePaymentResponse
import com.insidehealthgt.hms.dto.response.ExpenseResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/treasury/expenses")
class ExpenseController(private val expenseService: ExpenseService) {

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listExpenses(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) status: ExpenseStatus?,
        @RequestParam(required = false) category: ExpenseCategory?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> {
        val expenses = expenseService.findAll(status, category, from, to, search, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(expenses)))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getExpense(@PathVariable id: Long): ResponseEntity<ApiResponse<ExpenseResponse>> {
        val expense = expenseService.getById(id)
        return ResponseEntity.ok(ApiResponse.success(expense))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('treasury:write')")
    fun createExpense(
        @Valid @RequestBody request: CreateExpenseRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        val expense = expenseService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(expense))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun updateExpense(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateExpenseRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        val expense = expenseService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(expense))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:delete')")
    fun deleteExpense(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        expenseService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @PostMapping("/{id}/invoice-document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('treasury:write')")
    fun uploadInvoiceDocument(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        val expense = expenseService.uploadInvoiceDocument(id, file)
        return ResponseEntity.ok(ApiResponse.success(expense))
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun recordPayment(
        @PathVariable id: Long,
        @Valid @RequestBody request: RecordExpensePaymentRequest,
    ): ResponseEntity<ApiResponse<ExpensePaymentResponse>> {
        val payment = expenseService.recordPayment(id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payment))
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getPayments(@PathVariable id: Long): ResponseEntity<ApiResponse<List<ExpensePaymentResponse>>> {
        val payments = expenseService.getPayments(id)
        return ResponseEntity.ok(ApiResponse.success(payments))
    }
}
