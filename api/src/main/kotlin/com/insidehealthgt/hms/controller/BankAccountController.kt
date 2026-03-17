package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateBankAccountRequest
import com.insidehealthgt.hms.dto.request.UpdateBankAccountRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.BankAccountResponse
import com.insidehealthgt.hms.service.BankAccountService
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/treasury/bank-accounts")
class BankAccountController(private val bankAccountService: BankAccountService) {

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listBankAccounts(): ResponseEntity<ApiResponse<List<BankAccountResponse>>> {
        val accounts = bankAccountService.findAll()
        return ResponseEntity.ok(ApiResponse.success(accounts))
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listActiveBankAccounts(): ResponseEntity<ApiResponse<List<BankAccountResponse>>> {
        val accounts = bankAccountService.findAllActive()
        return ResponseEntity.ok(ApiResponse.success(accounts))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getBankAccount(@PathVariable id: Long): ResponseEntity<ApiResponse<BankAccountResponse>> {
        val account = bankAccountService.getById(id)
        return ResponseEntity.ok(ApiResponse.success(account))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun createBankAccount(
        @Valid @RequestBody request: CreateBankAccountRequest,
    ): ResponseEntity<ApiResponse<BankAccountResponse>> {
        val account = bankAccountService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(account))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun updateBankAccount(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBankAccountRequest,
    ): ResponseEntity<ApiResponse<BankAccountResponse>> {
        val account = bankAccountService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(account))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun deleteBankAccount(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        bankAccountService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
