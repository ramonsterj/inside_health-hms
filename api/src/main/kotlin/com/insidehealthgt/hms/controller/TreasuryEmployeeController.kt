package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.request.GeneratePayrollScheduleRequest
import com.insidehealthgt.hms.dto.request.RecordContractorPaymentRequest
import com.insidehealthgt.hms.dto.request.RecordPayrollPaymentRequest
import com.insidehealthgt.hms.dto.request.TerminateEmployeeRequest
import com.insidehealthgt.hms.dto.request.UpdateSalaryRequest
import com.insidehealthgt.hms.dto.request.UpdateTreasuryEmployeeRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.EmployeePaymentHistoryResponse
import com.insidehealthgt.hms.dto.response.ExpenseResponse
import com.insidehealthgt.hms.dto.response.IndemnizacionResponse
import com.insidehealthgt.hms.dto.response.PayrollEntryResponse
import com.insidehealthgt.hms.dto.response.SalaryHistoryResponse
import com.insidehealthgt.hms.dto.response.TreasuryEmployeeResponse
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.service.TreasuryEmployeeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("TooManyFunctions")
@RestController
@RequestMapping("/api/v1/treasury/employees")
class TreasuryEmployeeController(private val employeeService: TreasuryEmployeeService) {

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listEmployees(
        @RequestParam(required = false) type: EmployeeType?,
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<List<TreasuryEmployeeResponse>>> {
        val employees = employeeService.findAll(type, activeOnly, search)
        return ResponseEntity.ok(ApiResponse.success(employees))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getEmployee(@PathVariable id: Long): ResponseEntity<ApiResponse<TreasuryEmployeeResponse>> {
        val employee = employeeService.getById(id)
        return ResponseEntity.ok(ApiResponse.success(employee))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun createEmployee(
        @Valid @RequestBody request: CreateTreasuryEmployeeRequest,
    ): ResponseEntity<ApiResponse<TreasuryEmployeeResponse>> {
        val employee = employeeService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(employee))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun updateEmployee(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTreasuryEmployeeRequest,
    ): ResponseEntity<ApiResponse<TreasuryEmployeeResponse>> {
        val employee = employeeService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(employee))
    }

    @PutMapping("/{id}/salary")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun updateSalary(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSalaryRequest,
    ): ResponseEntity<ApiResponse<TreasuryEmployeeResponse>> {
        val employee = employeeService.updateSalary(id, request)
        return ResponseEntity.ok(ApiResponse.success(employee))
    }

    @GetMapping("/{id}/salary-history")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getSalaryHistory(@PathVariable id: Long): ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> {
        val history = employeeService.getSalaryHistory(id)
        return ResponseEntity.ok(ApiResponse.success(history))
    }

    @PostMapping("/{id}/payroll/generate")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun generatePayroll(
        @PathVariable id: Long,
        @Valid @RequestBody request: GeneratePayrollScheduleRequest,
    ): ResponseEntity<ApiResponse<List<PayrollEntryResponse>>> {
        val entries = employeeService.generatePayrollSchedule(id, request)
        return ResponseEntity.ok(ApiResponse.success(entries))
    }

    @GetMapping("/{id}/payroll")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getPayroll(
        @PathVariable id: Long,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<ApiResponse<List<PayrollEntryResponse>>> {
        val entries = if (year != null) {
            employeeService.getPayrollEntriesForYear(id, year)
        } else {
            employeeService.getPayrollEntries(id)
        }
        return ResponseEntity.ok(ApiResponse.success(entries))
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun recordContractorPayment(
        @PathVariable id: Long,
        @Valid @RequestBody request: RecordContractorPaymentRequest,
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        val expense = employeeService.recordContractorPayment(id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(expense))
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('treasury:configure')")
    fun terminateEmployee(
        @PathVariable id: Long,
        @Valid @RequestBody request: TerminateEmployeeRequest,
    ): ResponseEntity<ApiResponse<TreasuryEmployeeResponse>> {
        val employee = employeeService.terminate(id, request)
        return ResponseEntity.ok(ApiResponse.success(employee))
    }

    @GetMapping("/{id}/indemnizacion")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getIndemnizacion(@PathVariable id: Long): ResponseEntity<ApiResponse<IndemnizacionResponse>> {
        val result = employeeService.calculateIndemnizacion(id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/{id}/payment-history")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getPaymentHistory(@PathVariable id: Long): ResponseEntity<ApiResponse<List<EmployeePaymentHistoryResponse>>> {
        val history = employeeService.getPaymentHistory(id)
        return ResponseEntity.ok(ApiResponse.success(history))
    }

    @PostMapping("/payroll/{entryId}/pay")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun payPayrollEntry(
        @PathVariable entryId: Long,
        @Valid @RequestBody request: RecordPayrollPaymentRequest,
    ): ResponseEntity<ApiResponse<PayrollEntryResponse>> {
        val entry = employeeService.payPayrollEntry(entryId, request)
        return ResponseEntity.ok(ApiResponse.success(entry))
    }
}
