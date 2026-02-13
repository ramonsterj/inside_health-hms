package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.response.PsychotherapyCategoryResponse
import com.insidehealthgt.hms.entity.PsychotherapyCategory
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PsychotherapyCategoryService(
    private val categoryRepository: PsychotherapyCategoryRepository,
    private val activityRepository: PsychotherapyActivityRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): PsychotherapyCategory = categoryRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Psychotherapy category not found with id: $id") }

    @Transactional(readOnly = true)
    fun findAll(): List<PsychotherapyCategoryResponse> = categoryRepository.findAllByOrderByDisplayOrderAsc()
        .map { PsychotherapyCategoryResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun findAllActive(): List<PsychotherapyCategoryResponse> =
        categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
            .map { PsychotherapyCategoryResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun getCategory(id: Long): PsychotherapyCategoryResponse {
        val category = findById(id)
        return buildCategoryResponse(category)
    }

    @Transactional
    fun createCategory(request: CreatePsychotherapyCategoryRequest): PsychotherapyCategoryResponse {
        if (categoryRepository.existsByName(request.name)) {
            throw BadRequestException("Psychotherapy category with name '${request.name}' already exists")
        }

        val category = PsychotherapyCategory(
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder,
            active = request.active,
            price = request.price,
            cost = request.cost,
        )

        val savedCategory = categoryRepository.save(category)
        return buildCategoryResponse(savedCategory)
    }

    @Transactional
    fun updateCategory(id: Long, request: UpdatePsychotherapyCategoryRequest): PsychotherapyCategoryResponse {
        val category = findById(id)

        if (categoryRepository.existsByNameExcludingId(request.name, id)) {
            throw BadRequestException("Psychotherapy category with name '${request.name}' already exists")
        }

        category.name = request.name
        category.description = request.description
        category.displayOrder = request.displayOrder
        category.active = request.active
        category.price = request.price
        category.cost = request.cost

        val savedCategory = categoryRepository.save(category)
        return buildCategoryResponse(savedCategory)
    }

    @Transactional
    fun deleteCategory(id: Long) {
        val category = findById(id)

        if (activityRepository.existsByCategoryIdIncludingDeleted(id)) {
            throw BadRequestException("Cannot delete category that is in use by existing activities")
        }

        category.deletedAt = LocalDateTime.now()
        categoryRepository.save(category)
    }

    private fun buildCategoryResponse(category: PsychotherapyCategory): PsychotherapyCategoryResponse {
        val createdByUser = category.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = category.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return PsychotherapyCategoryResponse.from(category, createdByUser, updatedByUser)
    }
}
