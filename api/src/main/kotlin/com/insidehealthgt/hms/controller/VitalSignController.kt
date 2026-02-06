package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateVitalSignRequest
import com.insidehealthgt.hms.dto.request.UpdateVitalSignRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.VitalSignResponse
import com.insidehealthgt.hms.service.VitalSignService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/vital-signs")
class VitalSignController(private val vitalSignService: VitalSignService) {

    @GetMapping
    @PreAuthorize("hasAuthority('vital-sign:read')")
    fun listVitalSigns(
        @PathVariable admissionId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @PageableDefault(size = 20, sort = ["recordedAt"]) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<VitalSignResponse>>> {
        val vitalSigns = vitalSignService.listVitalSigns(admissionId, fromDate, toDate, pageable)
        return ResponseEntity.ok(ApiResponse.success(vitalSigns))
    }

    @GetMapping("/chart")
    @PreAuthorize("hasAuthority('vital-sign:read')")
    fun listVitalSignsForChart(
        @PathVariable admissionId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
    ): ResponseEntity<ApiResponse<List<VitalSignResponse>>> {
        val vitalSigns = vitalSignService.listVitalSignsForChart(admissionId, fromDate, toDate)
        return ResponseEntity.ok(ApiResponse.success(vitalSigns))
    }

    @GetMapping("/{vitalSignId}")
    @PreAuthorize("hasAuthority('vital-sign:read')")
    fun getVitalSign(
        @PathVariable admissionId: Long,
        @PathVariable vitalSignId: Long,
    ): ResponseEntity<ApiResponse<VitalSignResponse>> {
        val vitalSign = vitalSignService.getVitalSign(admissionId, vitalSignId)
        return ResponseEntity.ok(ApiResponse.success(vitalSign))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('vital-sign:create')")
    fun createVitalSign(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateVitalSignRequest,
    ): ResponseEntity<ApiResponse<VitalSignResponse>> {
        val vitalSign = vitalSignService.createVitalSign(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vitalSign))
    }

    @PutMapping("/{vitalSignId}")
    @PreAuthorize("hasAuthority('vital-sign:update')")
    fun updateVitalSign(
        @PathVariable admissionId: Long,
        @PathVariable vitalSignId: Long,
        @Valid @RequestBody request: UpdateVitalSignRequest,
    ): ResponseEntity<ApiResponse<VitalSignResponse>> {
        val vitalSign = vitalSignService.updateVitalSign(admissionId, vitalSignId, request)
        return ResponseEntity.ok(ApiResponse.success(vitalSign))
    }
}
