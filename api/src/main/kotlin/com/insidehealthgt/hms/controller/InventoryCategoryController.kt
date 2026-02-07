package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.InventoryCategoryResponse
import com.insidehealthgt.hms.service.InventoryCategoryService
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
@RequestMapping("/api/v1")
class InventoryCategoryController(private val categoryService: InventoryCategoryService) {

    /**
     * List active categories for dropdown.
     */
    @GetMapping("/inventory-categories")
    @PreAuthorize("hasAuthority('inventory-category:read')")
    fun listActiveCategories(): ResponseEntity<ApiResponse<List<InventoryCategoryResponse>>> {
        val categories = categoryService.findAllActive()
        return ResponseEntity.ok(ApiResponse.success(categories))
    }

    /**
     * List all categories including inactive (admin).
     */
    @GetMapping("/admin/inventory-categories")
    @PreAuthorize("hasAuthority('inventory-category:read')")
    fun listAllCategories(): ResponseEntity<ApiResponse<List<InventoryCategoryResponse>>> {
        val categories = categoryService.findAll()
        return ResponseEntity.ok(ApiResponse.success(categories))
    }

    /**
     * Get single category (admin).
     */
    @GetMapping("/admin/inventory-categories/{id}")
    @PreAuthorize("hasAuthority('inventory-category:read')")
    fun getCategory(@PathVariable id: Long): ResponseEntity<ApiResponse<InventoryCategoryResponse>> {
        val category = categoryService.getCategory(id)
        return ResponseEntity.ok(ApiResponse.success(category))
    }

    /**
     * Create category (admin).
     */
    @PostMapping("/admin/inventory-categories")
    @PreAuthorize("hasAuthority('inventory-category:create')")
    fun createCategory(
        @Valid @RequestBody request: CreateInventoryCategoryRequest,
    ): ResponseEntity<ApiResponse<InventoryCategoryResponse>> {
        val category = categoryService.createCategory(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(category))
    }

    /**
     * Update category (admin).
     */
    @PutMapping("/admin/inventory-categories/{id}")
    @PreAuthorize("hasAuthority('inventory-category:update')")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInventoryCategoryRequest,
    ): ResponseEntity<ApiResponse<InventoryCategoryResponse>> {
        val category = categoryService.updateCategory(id, request)
        return ResponseEntity.ok(ApiResponse.success(category))
    }

    /**
     * Delete category (admin). Cannot delete if items exist in category.
     */
    @DeleteMapping("/admin/inventory-categories/{id}")
    @PreAuthorize("hasAuthority('inventory-category:delete')")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<Unit> {
        categoryService.deleteCategory(id)
        return ResponseEntity.noContent().build()
    }
}
