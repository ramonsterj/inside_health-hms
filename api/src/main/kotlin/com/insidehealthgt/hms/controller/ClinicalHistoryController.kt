package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.request.UpdateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.ClinicalHistoryResponse
import com.insidehealthgt.hms.service.ClinicalHistoryService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/clinical-history")
class ClinicalHistoryController(private val clinicalHistoryService: ClinicalHistoryService) {

    @GetMapping
    @PreAuthorize("hasAuthority('clinical-history:read')")
    fun getClinicalHistory(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<ClinicalHistoryResponse?>> {
        val clinicalHistory = clinicalHistoryService.getClinicalHistory(admissionId)
        return ResponseEntity.ok(ApiResponse.success(clinicalHistory))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('clinical-history:create')")
    fun createClinicalHistory(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateClinicalHistoryRequest,
    ): ResponseEntity<ApiResponse<ClinicalHistoryResponse>> {
        val clinicalHistory = clinicalHistoryService.createClinicalHistory(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(clinicalHistory))
    }

    @PutMapping
    @PreAuthorize("hasAuthority('clinical-history:update')")
    fun updateClinicalHistory(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: UpdateClinicalHistoryRequest,
    ): ResponseEntity<ApiResponse<ClinicalHistoryResponse>> {
        val clinicalHistory = clinicalHistoryService.updateClinicalHistory(admissionId, request)
        return ResponseEntity.ok(ApiResponse.success(clinicalHistory))
    }
}
