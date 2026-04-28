package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderListItemResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.service.MedicalOrderService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Cross-admission listing for the orders-by-state dashboard at `/medical-orders` in the UI.
 * Lives outside the per-admission MedicalOrderController so the route does not have to mount
 * under `/api/v1/admissions/{admissionId}`.
 */
@RestController
@RequestMapping("/api/v1/medical-orders")
class MedicalOrderDashboardController(private val medicalOrderService: MedicalOrderService) {

    @GetMapping
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun listOrdersByStatus(
        @RequestParam(required = false) status: List<MedicalOrderStatus>?,
        @RequestParam(required = false) category: List<MedicalOrderCategory>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<PageResponse<MedicalOrderListItemResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val results = medicalOrderService.listOrdersByStatus(
            statuses = status?.takeIf { it.isNotEmpty() },
            categories = category?.takeIf { it.isNotEmpty() },
            pageable = pageable,
        )
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(results)))
    }
}
