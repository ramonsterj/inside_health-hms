package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdjustmentRequest
import com.insidehealthgt.hms.dto.request.CreateChargeRequest
import com.insidehealthgt.hms.dto.response.AdmissionBalanceResponse
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.InvoiceResponse
import com.insidehealthgt.hms.dto.response.PatientChargeResponse
import com.insidehealthgt.hms.service.BillingService
import com.insidehealthgt.hms.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}")
class BillingController(private val billingService: BillingService, private val invoiceService: InvoiceService) {

    @GetMapping("/charges")
    @PreAuthorize("hasAuthority('billing:read')")
    fun listCharges(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<List<PatientChargeResponse>>> {
        val charges = billingService.getCharges(admissionId)
        return ResponseEntity.ok(ApiResponse.success(charges))
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('billing:read')")
    fun getBalance(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<AdmissionBalanceResponse>> {
        val balance = billingService.getBalance(admissionId)
        return ResponseEntity.ok(ApiResponse.success(balance))
    }

    @PostMapping("/charges")
    @PreAuthorize("hasAuthority('billing:create')")
    fun createCharge(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateChargeRequest,
    ): ResponseEntity<ApiResponse<PatientChargeResponse>> {
        val charge = billingService.createManualCharge(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(charge))
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('billing:adjust')")
    fun createAdjustment(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateAdjustmentRequest,
    ): ResponseEntity<ApiResponse<PatientChargeResponse>> {
        val adjustment = billingService.createAdjustment(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(adjustment))
    }

    @GetMapping("/invoice")
    @PreAuthorize("hasAuthority('invoice:read')")
    fun getInvoice(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<InvoiceResponse>> {
        val invoice = invoiceService.getInvoice(admissionId)
        return ResponseEntity.ok(ApiResponse.success(invoice))
    }

    @PostMapping("/invoice")
    @PreAuthorize("hasAuthority('invoice:create')")
    fun generateInvoice(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<InvoiceResponse>> {
        val invoice = invoiceService.generateInvoice(admissionId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(invoice))
    }
}
