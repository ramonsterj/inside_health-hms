package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateWarehouseRequest
import com.insidehealthgt.hms.dto.request.UpdateWarehouseRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.dto.response.WarehouseResponse
import com.insidehealthgt.hms.dto.response.WarehouseStockResponse
import com.insidehealthgt.hms.service.WarehouseService
import jakarta.validation.Valid
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
@RequestMapping("/api/v1/warehouses")
class WarehouseController(private val warehouseService: WarehouseService) {

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:read')")
    fun list(): ResponseEntity<ApiResponse<List<WarehouseResponse>>> =
        ResponseEntity.ok(ApiResponse.success(warehouseService.list()))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:read')")
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<WarehouseResponse>> =
        ResponseEntity.ok(ApiResponse.success(warehouseService.get(id)))

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAuthority('warehouse:read')")
    fun stock(
        @PathVariable id: Long,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "false") lowStockOnly: Boolean,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<WarehouseStockResponse>>> {
        val page = warehouseService.getStock(id, search, lowStockOnly, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:create')")
    fun create(@Valid @RequestBody request: CreateWarehouseRequest): ResponseEntity<ApiResponse<WarehouseResponse>> {
        val created = warehouseService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:update')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateWarehouseRequest,
    ): ResponseEntity<ApiResponse<WarehouseResponse>> =
        ResponseEntity.ok(ApiResponse.success(warehouseService.update(id, request)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:delete')")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        warehouseService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
