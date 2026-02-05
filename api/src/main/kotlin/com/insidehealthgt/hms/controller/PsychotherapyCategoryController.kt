package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PsychotherapyCategoryResponse
import com.insidehealthgt.hms.service.PsychotherapyCategoryService
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
class PsychotherapyCategoryController(private val categoryService: PsychotherapyCategoryService) {

    /**
     * List active categories for dropdown (all clinical staff).
     */
    @GetMapping("/psychotherapy-categories")
    @PreAuthorize("hasAuthority('psychotherapy-category:read')")
    fun listActiveCategories(): ResponseEntity<ApiResponse<List<PsychotherapyCategoryResponse>>> {
        val categories = categoryService.findAllActive()
        return ResponseEntity.ok(ApiResponse.success(categories))
    }

    /**
     * List all categories including inactive (admin).
     */
    @GetMapping("/admin/psychotherapy-categories")
    @PreAuthorize("hasAuthority('psychotherapy-category:read')")
    fun listAllCategories(): ResponseEntity<ApiResponse<List<PsychotherapyCategoryResponse>>> {
        val categories = categoryService.findAll()
        return ResponseEntity.ok(ApiResponse.success(categories))
    }

    /**
     * Get single category (admin).
     */
    @GetMapping("/admin/psychotherapy-categories/{id}")
    @PreAuthorize("hasAuthority('psychotherapy-category:read')")
    fun getCategory(@PathVariable id: Long): ResponseEntity<ApiResponse<PsychotherapyCategoryResponse>> {
        val category = categoryService.getCategory(id)
        return ResponseEntity.ok(ApiResponse.success(category))
    }

    /**
     * Create category (admin).
     */
    @PostMapping("/admin/psychotherapy-categories")
    @PreAuthorize("hasAuthority('psychotherapy-category:create')")
    fun createCategory(
        @Valid @RequestBody request: CreatePsychotherapyCategoryRequest,
    ): ResponseEntity<ApiResponse<PsychotherapyCategoryResponse>> {
        val category = categoryService.createCategory(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(category))
    }

    /**
     * Update category (admin).
     */
    @PutMapping("/admin/psychotherapy-categories/{id}")
    @PreAuthorize("hasAuthority('psychotherapy-category:update')")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePsychotherapyCategoryRequest,
    ): ResponseEntity<ApiResponse<PsychotherapyCategoryResponse>> {
        val category = categoryService.updateCategory(id, request)
        return ResponseEntity.ok(ApiResponse.success(category))
    }

    /**
     * Delete category (admin). Cannot delete if in use by activities.
     */
    @DeleteMapping("/admin/psychotherapy-categories/{id}")
    @PreAuthorize("hasAuthority('psychotherapy-category:delete')")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<Void> {
        categoryService.deleteCategory(id)
        return ResponseEntity.noContent().build()
    }
}
