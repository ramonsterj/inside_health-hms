package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicationAdministrationRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.MedicationAdministrationResponse
import com.insidehealthgt.hms.service.MedicationAdministrationService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/medical-orders/{orderId}/administrations")
class MedicationAdministrationController(private val administrationService: MedicationAdministrationService) {

    @GetMapping
    @PreAuthorize("hasAuthority('medication-administration:read')")
    fun listAdministrations(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<MedicationAdministrationResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "administeredAt"))
        val administrations = administrationService.listAdministrations(admissionId, orderId, pageable)
        return ResponseEntity.ok(ApiResponse.success(administrations))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('medication-administration:create')")
    fun createAdministration(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @Valid @RequestBody request: CreateMedicationAdministrationRequest,
    ): ResponseEntity<ApiResponse<MedicationAdministrationResponse>> {
        val administration = administrationService.createAdministration(admissionId, orderId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(administration))
    }
}
