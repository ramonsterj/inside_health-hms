package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicationRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicationRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.ExpiryReportResponse
import com.insidehealthgt.hms.dto.response.InventoryLotResponse
import com.insidehealthgt.hms.dto.response.MedicationResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.MedicationSection
import com.insidehealthgt.hms.service.ExpiryReportService
import com.insidehealthgt.hms.service.PharmacyService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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

@RestController
@RequestMapping("/api/v1/medications")
class PharmacyController(
    private val pharmacyService: PharmacyService,
    private val expiryReportService: ExpiryReportService,
) {

    @GetMapping
    @PreAuthorize("hasAuthority('medication:read')")
    fun list(
        @RequestParam(required = false) section: MedicationSection?,
        @RequestParam(required = false) controlled: Boolean?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<MedicationResponse>>> {
        val page = pharmacyService.listMedications(section, controlled, search, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)))
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('medication:read')")
    fun get(@PathVariable itemId: Long): ResponseEntity<ApiResponse<MedicationResponse>> =
        ResponseEntity.ok(ApiResponse.success(pharmacyService.getMedication(itemId)))

    @PostMapping
    @PreAuthorize("hasAuthority('medication:create')")
    fun create(@Valid @RequestBody request: CreateMedicationRequest): ResponseEntity<ApiResponse<MedicationResponse>> {
        val created = pharmacyService.createMedication(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('medication:update')")
    fun update(
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateMedicationRequest,
    ): ResponseEntity<ApiResponse<MedicationResponse>> =
        ResponseEntity.ok(ApiResponse.success(pharmacyService.updateMedication(itemId, request)))

    @GetMapping("/expiry-report")
    @PreAuthorize("hasAuthority('medication:expiry-report')")
    fun expiryReport(
        @RequestParam(defaultValue = "90") window: Int,
        @RequestParam(defaultValue = "30") urgentWindow: Int,
        @RequestParam(required = false) section: MedicationSection?,
        @RequestParam(required = false) controlled: Boolean?,
    ): ResponseEntity<ApiResponse<ExpiryReportResponse>> {
        val report = expiryReportService.build(window, urgentWindow, section, controlled)
        return ResponseEntity.ok(ApiResponse.success(report))
    }

    @GetMapping("/{itemId}/fefo-preview")
    @PreAuthorize("hasAuthority('medication:read')")
    fun fefoPreview(
        @PathVariable itemId: Long,
        @RequestParam(defaultValue = "1") quantity: Int,
    ): ResponseEntity<ApiResponse<InventoryLotResponse?>> =
        ResponseEntity.ok(ApiResponse.success(pharmacyService.fefoPreview(itemId, quantity)))
}
