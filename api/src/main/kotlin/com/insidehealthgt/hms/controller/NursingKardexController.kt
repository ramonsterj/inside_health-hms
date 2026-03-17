package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.KardexAdmissionSummary
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.service.NursingKardexService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/nursing-kardex")
class NursingKardexController(private val nursingKardexService: NursingKardexService) {

    @GetMapping
    @PreAuthorize("hasAuthority('admission:read')")
    fun listKardex(
        @RequestParam(required = false) type: AdmissionType?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<KardexAdmissionSummary>>> {
        val summaries = nursingKardexService.getKardexSummaries(type, search, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(summaries)))
    }

    @GetMapping("/{admissionId}")
    @PreAuthorize("hasAuthority('admission:read')")
    fun getKardexSummary(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<KardexAdmissionSummary>> {
        val summary = nursingKardexService.getKardexSummary(admissionId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
}
