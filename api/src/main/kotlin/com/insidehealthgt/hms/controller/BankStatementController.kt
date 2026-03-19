package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AcknowledgeRowRequest
import com.insidehealthgt.hms.dto.request.CreateExpenseFromRowRequest
import com.insidehealthgt.hms.dto.request.CreateIncomeFromRowRequest
import com.insidehealthgt.hms.dto.request.MatchRowRequest
import com.insidehealthgt.hms.dto.request.SaveColumnMappingRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.BankStatementResponse
import com.insidehealthgt.hms.dto.response.BankStatementRowResponse
import com.insidehealthgt.hms.dto.response.ColumnMappingResponse
import com.insidehealthgt.hms.service.BankStatementService
import com.insidehealthgt.hms.service.ReconciliationService
import jakarta.validation.Valid
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

@Suppress("TooManyFunctions")
@RestController
@RequestMapping("/api/v1/treasury/bank-accounts/{bankAccountId}/statements")
class BankStatementController(
    private val bankStatementService: BankStatementService,
    private val reconciliationService: ReconciliationService,
) {

    @GetMapping("/column-mapping")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun getColumnMapping(@PathVariable bankAccountId: Long): ResponseEntity<ApiResponse<ColumnMappingResponse?>> {
        val mapping = bankStatementService.getColumnMapping(bankAccountId)
        return ResponseEntity.ok(ApiResponse.success(mapping))
    }

    @PutMapping("/column-mapping")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun saveColumnMapping(
        @PathVariable bankAccountId: Long,
        @Valid @RequestBody request: SaveColumnMappingRequest,
    ): ResponseEntity<ApiResponse<ColumnMappingResponse>> {
        val mapping = bankStatementService.saveColumnMapping(bankAccountId, request)
        return ResponseEntity.ok(ApiResponse.success(mapping))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun uploadStatement(
        @PathVariable bankAccountId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("statementDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) statementDate: LocalDate,
    ): ResponseEntity<ApiResponse<BankStatementResponse>> {
        val statement = bankStatementService.uploadStatement(bankAccountId, file, statementDate)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(statement))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun listStatements(@PathVariable bankAccountId: Long): ResponseEntity<ApiResponse<List<BankStatementResponse>>> {
        val statements = bankStatementService.getStatements(bankAccountId)
        return ResponseEntity.ok(ApiResponse.success(statements))
    }

    @GetMapping("/{statementId}")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun getStatement(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
    ): ResponseEntity<ApiResponse<BankStatementResponse>> {
        val statement = bankStatementService.getStatement(bankAccountId, statementId)
        return ResponseEntity.ok(ApiResponse.success(statement))
    }

    @DeleteMapping("/{statementId}")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun deleteStatement(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        bankStatementService.deleteStatement(bankAccountId, statementId)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @GetMapping("/{statementId}/rows")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun getRows(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
    ): ResponseEntity<ApiResponse<List<BankStatementRowResponse>>> {
        val rows = bankStatementService.getStatementRows(bankAccountId, statementId)
        return ResponseEntity.ok(ApiResponse.success(rows))
    }

    @PostMapping("/{statementId}/rows/{rowId}/confirm")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun confirmMatch(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.confirmMatch(bankAccountId, statementId, rowId)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/rows/{rowId}/reject")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun rejectMatch(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.rejectMatch(bankAccountId, statementId, rowId)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/rows/{rowId}/match")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun manualMatch(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
        @Valid @RequestBody request: MatchRowRequest,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.manualMatch(bankAccountId, statementId, rowId, request)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/rows/{rowId}/acknowledge")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun acknowledgeRow(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
        @Valid @RequestBody request: AcknowledgeRowRequest,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.acknowledgeRow(bankAccountId, statementId, rowId, request)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/rows/{rowId}/create-expense")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun createExpenseFromRow(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
        @Valid @RequestBody request: CreateExpenseFromRowRequest,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.createExpenseAndMatch(bankAccountId, statementId, rowId, request)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/rows/{rowId}/create-income")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun createIncomeFromRow(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
        @PathVariable rowId: Long,
        @Valid @RequestBody request: CreateIncomeFromRowRequest,
    ): ResponseEntity<ApiResponse<BankStatementRowResponse>> {
        val row = reconciliationService.createIncomeAndMatch(bankAccountId, statementId, rowId, request)
        return ResponseEntity.ok(ApiResponse.success(row))
    }

    @PostMapping("/{statementId}/complete")
    @PreAuthorize("hasAuthority('treasury:reconcile')")
    fun completeStatement(
        @PathVariable bankAccountId: Long,
        @PathVariable statementId: Long,
    ): ResponseEntity<ApiResponse<BankStatementResponse>> {
        val statement = bankStatementService.completeStatement(bankAccountId, statementId)
        return ResponseEntity.ok(ApiResponse.success(statement))
    }
}
