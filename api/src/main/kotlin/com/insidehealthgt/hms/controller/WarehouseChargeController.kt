package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateWarehouseChargeRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.dto.response.WarehouseChargeResponse
import com.insidehealthgt.hms.service.WarehouseChargeService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/warehouse-charges")
class WarehouseChargeController(private val chargeService: WarehouseChargeService) {

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse-charge:create')")
    fun create(
        @Valid @RequestBody request: CreateWarehouseChargeRequest,
    ): ResponseEntity<ApiResponse<WarehouseChargeResponse>> {
        val created = chargeService.createCharge(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse-charge:create')")
    fun list(
        @RequestParam(required = false) warehouseId: Long?,
        @RequestParam(required = false) admissionId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<WarehouseChargeResponse>>> {
        val page = chargeService.listCharges(warehouseId, admissionId, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)))
    }
}
