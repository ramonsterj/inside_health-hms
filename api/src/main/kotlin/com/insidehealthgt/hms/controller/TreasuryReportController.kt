package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.BankAccountSummaryResponse
import com.insidehealthgt.hms.dto.response.EmployeeCompensationResponse
import com.insidehealthgt.hms.dto.response.IndemnizacionLiabilityResponse
import com.insidehealthgt.hms.dto.response.MonthlyPaymentReportResponse
import com.insidehealthgt.hms.dto.response.ReconciliationSummaryResponse
import com.insidehealthgt.hms.dto.response.TreasuryDashboardResponse
import com.insidehealthgt.hms.dto.response.UpcomingPaymentsResponse
import com.insidehealthgt.hms.service.TreasuryReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/treasury/reports")
@PreAuthorize("hasAuthority('treasury:report')")
class TreasuryReportController(private val reportService: TreasuryReportService) {

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<ApiResponse<TreasuryDashboardResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getDashboard()))

    @GetMapping("/monthly")
    fun getMonthlyReport(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): ResponseEntity<ApiResponse<MonthlyPaymentReportResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyReport(from, to)))

    @GetMapping("/upcoming-payments")
    fun getUpcomingPayments(
        @RequestParam(defaultValue = "30") windowDays: Int,
    ): ResponseEntity<ApiResponse<UpcomingPaymentsResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getUpcomingPayments(windowDays)))

    @GetMapping("/bank-summary")
    fun getBankAccountSummary(): ResponseEntity<ApiResponse<BankAccountSummaryResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getBankAccountSummary()))

    @GetMapping("/compensation")
    fun getEmployeeCompensation(
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<ApiResponse<EmployeeCompensationResponse>> {
        val reportYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(ApiResponse.success(reportService.getEmployeeCompensation(reportYear)))
    }

    @GetMapping("/indemnizacion")
    fun getIndemnizacionLiability(): ResponseEntity<ApiResponse<IndemnizacionLiabilityResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getIndemnizacionLiability()))

    @GetMapping("/reconciliation")
    fun getReconciliationSummary(): ResponseEntity<ApiResponse<ReconciliationSummaryResponse>> =
        ResponseEntity.ok(ApiResponse.success(reportService.getReconciliationSummary()))
}
