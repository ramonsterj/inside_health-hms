@file:Suppress("FunctionSignature") // Multiline parameter style for endpoint handlers

package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateLabPanelRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.CreateLabTestRequest
import com.insidehealthgt.hms.dto.request.UpdateLabPanelRequest
import com.insidehealthgt.hms.dto.request.UpdateLabProviderRequest
import com.insidehealthgt.hms.dto.request.UpdateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.UpdateLabTestRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.LabPanelResponse
import com.insidehealthgt.hms.dto.response.LabProviderResponse
import com.insidehealthgt.hms.dto.response.LabProviderTestResponse
import com.insidehealthgt.hms.dto.response.LabTestResponse
import com.insidehealthgt.hms.dto.response.PanelResolutionResponse
import com.insidehealthgt.hms.service.LabCatalogService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/lab")
@Suppress("TooManyFunctions")
class LabCatalogController(private val labCatalogService: LabCatalogService) {

    // ===================== Providers =====================

    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('lab-catalog:read')")
    fun listProviders(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<ApiResponse<List<LabProviderResponse>>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.listProviders(activeOnly)))

    @PostMapping("/providers")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun createProvider(
        @Valid @RequestBody request: CreateLabProviderRequest,
    ): ResponseEntity<ApiResponse<LabProviderResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(labCatalogService.createProvider(request)))

    @PutMapping("/providers/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun updateProvider(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLabProviderRequest,
    ): ResponseEntity<ApiResponse<LabProviderResponse>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.updateProvider(id, request)))

    @DeleteMapping("/providers/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun deleteProvider(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        labCatalogService.deleteProvider(id)
        return ResponseEntity.ok(ApiResponse.success("Lab provider deleted successfully"))
    }

    // ===================== Canonical tests =====================

    @GetMapping("/tests")
    @PreAuthorize("hasAuthority('lab-catalog:read')")
    fun listTests(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<ApiResponse<List<LabTestResponse>>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.listTests(activeOnly)))

    @PostMapping("/tests")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun createTest(
        @Valid @RequestBody request: CreateLabTestRequest,
    ): ResponseEntity<ApiResponse<LabTestResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(labCatalogService.createTest(request)))

    @PutMapping("/tests/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun updateTest(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLabTestRequest,
    ): ResponseEntity<ApiResponse<LabTestResponse>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.updateTest(id, request)))

    @DeleteMapping("/tests/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun deleteTest(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        labCatalogService.deleteTest(id)
        return ResponseEntity.ok(ApiResponse.success("Lab test deleted successfully"))
    }

    // ===================== Provider tests =====================

    @GetMapping("/providers/{providerId}/tests")
    @PreAuthorize("hasAuthority('lab-catalog:read')")
    fun listProviderTests(
        @PathVariable providerId: Long,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<ApiResponse<List<LabProviderTestResponse>>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.listProviderTests(providerId, activeOnly)))

    @PostMapping("/providers/{providerId}/tests")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun createProviderTest(
        @PathVariable providerId: Long,
        @Valid @RequestBody request: CreateLabProviderTestRequest,
    ): ResponseEntity<ApiResponse<LabProviderTestResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(labCatalogService.createProviderTest(providerId, request)))

    @PutMapping("/provider-tests/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun updateProviderTest(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLabProviderTestRequest,
    ): ResponseEntity<ApiResponse<LabProviderTestResponse>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.updateProviderTest(id, request)))

    @DeleteMapping("/provider-tests/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun deleteProviderTest(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        labCatalogService.deleteProviderTest(id)
        return ResponseEntity.ok(ApiResponse.success("Lab provider test deleted successfully"))
    }

    // ===================== Panels =====================

    @GetMapping("/panels")
    @PreAuthorize("hasAuthority('lab-catalog:read')")
    fun listPanels(): ResponseEntity<ApiResponse<List<LabPanelResponse>>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.listPanels()))

    @PostMapping("/panels")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun createPanel(
        @Valid @RequestBody request: CreateLabPanelRequest,
    ): ResponseEntity<ApiResponse<LabPanelResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(labCatalogService.createPanel(request)))

    @PutMapping("/panels/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun updatePanel(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLabPanelRequest,
    ): ResponseEntity<ApiResponse<LabPanelResponse>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.updatePanel(id, request)))

    @DeleteMapping("/panels/{id}")
    @PreAuthorize("hasAuthority('lab-catalog:manage')")
    fun deletePanel(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        labCatalogService.deletePanel(id)
        return ResponseEntity.ok(ApiResponse.success("Lab panel deleted successfully"))
    }

    @GetMapping("/panels/{panelId}/resolve")
    @PreAuthorize("hasAuthority('lab-catalog:read')")
    fun resolvePanel(
        @PathVariable panelId: Long,
        @RequestParam providerId: Long,
    ): ResponseEntity<ApiResponse<PanelResolutionResponse>> =
        ResponseEntity.ok(ApiResponse.success(labCatalogService.resolvePanel(panelId, providerId)))
}
