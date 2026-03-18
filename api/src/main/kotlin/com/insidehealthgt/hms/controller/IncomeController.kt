package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateIncomeRequest
import com.insidehealthgt.hms.dto.request.UpdateIncomeRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.IncomeResponse
import com.insidehealthgt.hms.dto.response.InvoiceSummaryResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.service.IncomeService
import com.insidehealthgt.hms.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/treasury/income")
class IncomeController(private val incomeService: IncomeService, private val invoiceService: InvoiceService) {

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listIncome(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) category: IncomeCategory?,
        @RequestParam(required = false) bankAccountId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<PageResponse<IncomeResponse>>> {
        val income = incomeService.findAll(category, bankAccountId, from, to, search, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(income)))
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun searchInvoices(
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>> {
        val invoices = invoiceService.searchInvoices(search)
        return ResponseEntity.ok(ApiResponse.success(invoices))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getIncome(@PathVariable id: Long): ResponseEntity<ApiResponse<IncomeResponse>> {
        val income = incomeService.getById(id)
        return ResponseEntity.ok(ApiResponse.success(income))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('treasury:write')")
    fun createIncome(@Valid @RequestBody request: CreateIncomeRequest): ResponseEntity<ApiResponse<IncomeResponse>> {
        val income = incomeService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(income))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun updateIncome(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateIncomeRequest,
    ): ResponseEntity<ApiResponse<IncomeResponse>> {
        val income = incomeService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(income))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:delete')")
    fun deleteIncome(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        incomeService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
