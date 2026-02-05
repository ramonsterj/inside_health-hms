package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.GroupedMedicalOrdersResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderResponse
import com.insidehealthgt.hms.service.MedicalOrderService
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
@RequestMapping("/api/v1/admissions/{admissionId}/medical-orders")
class MedicalOrderController(private val medicalOrderService: MedicalOrderService) {

    @GetMapping
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun listMedicalOrders(@PathVariable admissionId: Long): ResponseEntity<ApiResponse<GroupedMedicalOrdersResponse>> {
        val orders = medicalOrderService.listMedicalOrders(admissionId)
        return ResponseEntity.ok(ApiResponse.success(orders))
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun getMedicalOrder(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
    ): ResponseEntity<ApiResponse<MedicalOrderResponse>> {
        val order = medicalOrderService.getMedicalOrder(admissionId, orderId)
        return ResponseEntity.ok(ApiResponse.success(order))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('medical-order:create')")
    fun createMedicalOrder(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateMedicalOrderRequest,
    ): ResponseEntity<ApiResponse<MedicalOrderResponse>> {
        val order = medicalOrderService.createMedicalOrder(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order))
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasAuthority('medical-order:update')")
    fun updateMedicalOrder(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @Valid @RequestBody request: UpdateMedicalOrderRequest,
    ): ResponseEntity<ApiResponse<MedicalOrderResponse>> {
        val order = medicalOrderService.updateMedicalOrder(admissionId, orderId, request)
        return ResponseEntity.ok(ApiResponse.success(order))
    }

    @PostMapping("/{orderId}/discontinue")
    @PreAuthorize("hasAuthority('medical-order:discontinue')")
    fun discontinueMedicalOrder(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
    ): ResponseEntity<ApiResponse<MedicalOrderResponse>> {
        val order = medicalOrderService.discontinueMedicalOrder(admissionId, orderId)
        return ResponseEntity.ok(ApiResponse.success(order))
    }
}
