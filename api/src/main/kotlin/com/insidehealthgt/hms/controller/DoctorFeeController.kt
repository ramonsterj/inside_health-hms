package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.SettleDoctorFeeRequest
import com.insidehealthgt.hms.dto.request.UpdateDoctorFeeStatusRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.DoctorFeeResponse
import com.insidehealthgt.hms.dto.response.DoctorFeeSummaryResponse
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import com.insidehealthgt.hms.service.DoctorFeeService
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

@RestController
@RequestMapping("/api/v1/treasury/employees/{employeeId}/doctor-fees")
class DoctorFeeController(private val doctorFeeService: DoctorFeeService) {

    @GetMapping
    @PreAuthorize("hasAuthority('treasury:read')")
    fun listFees(
        @PathVariable employeeId: Long,
        @RequestParam(required = false) status: DoctorFeeStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<ApiResponse<List<DoctorFeeResponse>>> {
        val fees = doctorFeeService.findAll(employeeId, status, from, to)
        return ResponseEntity.ok(ApiResponse.success(fees))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getFee(
        @PathVariable employeeId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<DoctorFeeResponse>> {
        val fee = doctorFeeService.findById(employeeId, id)
        return ResponseEntity.ok(ApiResponse.success(fee))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('treasury:write')")
    fun createFee(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: CreateDoctorFeeRequest,
    ): ResponseEntity<ApiResponse<DoctorFeeResponse>> {
        val fee = doctorFeeService.create(employeeId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(fee))
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun updateStatus(
        @PathVariable employeeId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateDoctorFeeStatusRequest,
    ): ResponseEntity<ApiResponse<DoctorFeeResponse>> {
        val fee = doctorFeeService.updateStatus(employeeId, id, request)
        return ResponseEntity.ok(ApiResponse.success(fee))
    }

    @PostMapping("/{id}/invoice-document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('treasury:write')")
    fun uploadInvoiceDocument(
        @PathVariable employeeId: Long,
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse<DoctorFeeResponse>> {
        val fee = doctorFeeService.uploadInvoiceDocument(employeeId, id, file)
        return ResponseEntity.ok(ApiResponse.success(fee))
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAuthority('treasury:write')")
    fun settleFee(
        @PathVariable employeeId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: SettleDoctorFeeRequest,
    ): ResponseEntity<ApiResponse<DoctorFeeResponse>> {
        val fee = doctorFeeService.settle(employeeId, id, request)
        return ResponseEntity.ok(ApiResponse.success(fee))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('treasury:delete')")
    fun deleteFee(@PathVariable employeeId: Long, @PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        doctorFeeService.delete(employeeId, id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('treasury:read')")
    fun getSummary(@PathVariable employeeId: Long): ResponseEntity<ApiResponse<DoctorFeeSummaryResponse>> {
        val summary = doctorFeeService.getSummary(employeeId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
}
