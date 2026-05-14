package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryLotRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryLotRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.InventoryLotResponse
import com.insidehealthgt.hms.service.InventoryLotService
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
@RequestMapping("/api/v1/inventory")
class InventoryLotController(private val lotService: InventoryLotService) {

    @GetMapping("/items/{itemId}/lots")
    @PreAuthorize("hasAuthority('inventory-lot:read')")
    fun listByItem(@PathVariable itemId: Long): ResponseEntity<ApiResponse<List<InventoryLotResponse>>> =
        ResponseEntity.ok(ApiResponse.success(lotService.listByItem(itemId)))

    @PostMapping("/items/{itemId}/lots")
    @PreAuthorize("hasAuthority('inventory-lot:create')")
    fun create(
        @PathVariable itemId: Long,
        @Valid @RequestBody request: CreateInventoryLotRequest,
    ): ResponseEntity<ApiResponse<InventoryLotResponse>> = ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(lotService.createLot(itemId, request)))

    @PutMapping("/lots/{lotId}")
    @PreAuthorize("hasAuthority('inventory-lot:update')")
    fun update(
        @PathVariable lotId: Long,
        @Valid @RequestBody request: UpdateInventoryLotRequest,
    ): ResponseEntity<ApiResponse<InventoryLotResponse>> =
        ResponseEntity.ok(ApiResponse.success(lotService.updateLot(lotId, request)))

    @DeleteMapping("/lots/{lotId}")
    @PreAuthorize("hasAuthority('inventory-lot:update')")
    fun delete(@PathVariable lotId: Long): ResponseEntity<Unit> {
        lotService.deleteLot(lotId)
        return ResponseEntity.noContent().build()
    }
}
