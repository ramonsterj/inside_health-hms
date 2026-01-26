package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateTriageCodeRequest
import com.insidehealthgt.hms.dto.request.UpdateTriageCodeRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.TriageCodeResponse
import com.insidehealthgt.hms.service.TriageCodeService
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
@RequestMapping("/api/v1/triage-codes")
class TriageCodeController(private val triageCodeService: TriageCodeService) {

    @GetMapping
    @PreAuthorize("hasAuthority('triage-code:read')")
    fun listTriageCodes(): ResponseEntity<ApiResponse<List<TriageCodeResponse>>> {
        val triageCodes = triageCodeService.findAll()
        return ResponseEntity.ok(ApiResponse.success(triageCodes))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('triage-code:read')")
    fun getTriageCode(@PathVariable id: Long): ResponseEntity<ApiResponse<TriageCodeResponse>> {
        val triageCode = triageCodeService.getTriageCode(id)
        return ResponseEntity.ok(ApiResponse.success(triageCode))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('triage-code:create')")
    fun createTriageCode(
        @Valid @RequestBody request: CreateTriageCodeRequest,
    ): ResponseEntity<ApiResponse<TriageCodeResponse>> {
        val triageCode = triageCodeService.createTriageCode(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(triageCode))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('triage-code:update')")
    fun updateTriageCode(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTriageCodeRequest,
    ): ResponseEntity<ApiResponse<TriageCodeResponse>> {
        val triageCode = triageCodeService.updateTriageCode(id, request)
        return ResponseEntity.ok(ApiResponse.success(triageCode))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('triage-code:delete')")
    fun deleteTriageCode(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        triageCodeService.deleteTriageCode(id)
        return ResponseEntity.ok(ApiResponse.success("Triage code deleted successfully"))
    }
}
