package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryItemRequest
import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryItemRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.InventoryItemResponse
import com.insidehealthgt.hms.dto.response.InventoryMovementResponse
import com.insidehealthgt.hms.service.InventoryItemService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
@RequestMapping("/api/v1/admin/inventory-items")
class InventoryItemController(private val itemService: InventoryItemService) {

    /**
     * List items with optional category filter and name search.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('inventory-item:read')")
    fun listItems(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<Page<InventoryItemResponse>>> {
        val items = itemService.findAll(categoryId, search, pageable)
        return ResponseEntity.ok(ApiResponse.success(items))
    }

    /**
     * Get single item.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory-item:read')")
    fun getItem(@PathVariable id: Long): ResponseEntity<ApiResponse<InventoryItemResponse>> {
        val item = itemService.getItem(id)
        return ResponseEntity.ok(ApiResponse.success(item))
    }

    /**
     * Create item.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('inventory-item:create')")
    fun createItem(
        @Valid @RequestBody request: CreateInventoryItemRequest,
    ): ResponseEntity<ApiResponse<InventoryItemResponse>> {
        val item = itemService.createItem(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(item))
    }

    /**
     * Update item.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory-item:update')")
    fun updateItem(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInventoryItemRequest,
    ): ResponseEntity<ApiResponse<InventoryItemResponse>> {
        val item = itemService.updateItem(id, request)
        return ResponseEntity.ok(ApiResponse.success(item))
    }

    /**
     * Delete item (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory-item:delete')")
    fun deleteItem(@PathVariable id: Long): ResponseEntity<Unit> {
        itemService.deleteItem(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Low stock report.
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory-item:read')")
    fun getLowStock(
        @RequestParam(required = false) categoryId: Long?,
    ): ResponseEntity<ApiResponse<List<InventoryItemResponse>>> {
        val items = itemService.findLowStock(categoryId)
        return ResponseEntity.ok(ApiResponse.success(items))
    }

    /**
     * List movements for an item.
     */
    @GetMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory-movement:read')")
    fun listMovements(@PathVariable itemId: Long): ResponseEntity<ApiResponse<List<InventoryMovementResponse>>> {
        val movements = itemService.getMovements(itemId)
        return ResponseEntity.ok(ApiResponse.success(movements))
    }

    /**
     * Record a stock movement.
     */
    @PostMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory-movement:create')")
    fun createMovement(
        @PathVariable itemId: Long,
        @Valid @RequestBody request: CreateInventoryMovementRequest,
    ): ResponseEntity<ApiResponse<InventoryMovementResponse>> {
        val movement = itemService.createMovement(itemId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(movement))
    }
}
