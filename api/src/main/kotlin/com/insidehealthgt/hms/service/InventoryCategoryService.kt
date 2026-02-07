package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.response.InventoryCategoryResponse
import com.insidehealthgt.hms.entity.InventoryCategory
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryCategoryRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InventoryCategoryService(
    private val categoryRepository: InventoryCategoryRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): InventoryCategory = categoryRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Inventory category not found with id: $id") }

    @Transactional(readOnly = true)
    fun findAll(): List<InventoryCategoryResponse> = categoryRepository.findAllByOrderByDisplayOrderAsc()
        .map { InventoryCategoryResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun findAllActive(): List<InventoryCategoryResponse> =
        categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
            .map { InventoryCategoryResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun getCategory(id: Long): InventoryCategoryResponse {
        val category = findById(id)
        return buildCategoryResponse(category)
    }

    @Transactional
    fun createCategory(request: CreateInventoryCategoryRequest): InventoryCategoryResponse {
        if (categoryRepository.existsByName(request.name)) {
            throw BadRequestException("Inventory category with name '${request.name}' already exists")
        }

        val category = InventoryCategory(
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder,
            active = request.active,
        )

        val savedCategory = categoryRepository.save(category)
        return buildCategoryResponse(savedCategory)
    }

    @Transactional
    fun updateCategory(id: Long, request: UpdateInventoryCategoryRequest): InventoryCategoryResponse {
        val category = findById(id)

        if (categoryRepository.existsByNameExcludingId(request.name, id)) {
            throw BadRequestException("Inventory category with name '${request.name}' already exists")
        }

        category.name = request.name
        category.description = request.description
        category.displayOrder = request.displayOrder
        category.active = request.active

        val savedCategory = categoryRepository.save(category)
        return buildCategoryResponse(savedCategory)
    }

    @Transactional
    fun deleteCategory(id: Long) {
        val category = findById(id)

        if (categoryRepository.existsItemsByCategoryIdIncludingDeleted(id)) {
            throw BadRequestException("Cannot delete category that has inventory items")
        }

        category.deletedAt = LocalDateTime.now()
        categoryRepository.save(category)
    }

    private fun buildCategoryResponse(category: InventoryCategory): InventoryCategoryResponse {
        val createdByUser = category.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = category.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return InventoryCategoryResponse.from(category, createdByUser, updatedByUser)
    }
}
