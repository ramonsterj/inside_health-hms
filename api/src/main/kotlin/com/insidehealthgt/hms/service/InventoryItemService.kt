package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryItemRequest
import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryItemRequest
import com.insidehealthgt.hms.dto.response.InventoryItemResponse
import com.insidehealthgt.hms.dto.response.InventoryMovementResponse
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryMovement
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryCategoryRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryMovementRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InventoryItemService(
    private val itemRepository: InventoryItemRepository,
    private val categoryRepository: InventoryCategoryRepository,
    private val movementRepository: InventoryMovementRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): InventoryItem = itemRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $id") }

    @Transactional(readOnly = true)
    fun findAll(categoryId: Long?, search: String?, pageable: Pageable): Page<InventoryItemResponse> =
        itemRepository.findAllWithFilters(categoryId, search?.trim() ?: "", pageable)
            .map { InventoryItemResponse.from(it) }

    @Transactional(readOnly = true)
    fun getItem(id: Long): InventoryItemResponse {
        val item = findById(id)
        return buildItemResponse(item)
    }

    @Transactional
    fun createItem(request: CreateInventoryItemRequest): InventoryItemResponse {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { BadRequestException("Inventory category not found with id: ${request.categoryId}") }

        validateTimeBased(request.pricingType, request.timeUnit, request.timeInterval)

        val item = InventoryItem(
            category = category,
            name = request.name,
            description = request.description,
            price = request.price,
            cost = request.cost,
            restockLevel = request.restockLevel,
            pricingType = request.pricingType,
            timeUnit = if (request.pricingType == PricingType.TIME_BASED) request.timeUnit else null,
            timeInterval = if (request.pricingType == PricingType.TIME_BASED) request.timeInterval else null,
            active = request.active,
        )

        val savedItem = itemRepository.save(item)
        return buildItemResponse(savedItem)
    }

    @Transactional
    fun updateItem(id: Long, request: UpdateInventoryItemRequest): InventoryItemResponse {
        val item = findById(id)

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { BadRequestException("Inventory category not found with id: ${request.categoryId}") }

        validateTimeBased(request.pricingType, request.timeUnit, request.timeInterval)

        item.category = category
        item.name = request.name
        item.description = request.description
        item.price = request.price
        item.cost = request.cost
        item.restockLevel = request.restockLevel
        item.pricingType = request.pricingType
        item.timeUnit = if (request.pricingType == PricingType.TIME_BASED) request.timeUnit else null
        item.timeInterval = if (request.pricingType == PricingType.TIME_BASED) request.timeInterval else null
        item.active = request.active

        val savedItem = itemRepository.save(item)
        return buildItemResponse(savedItem)
    }

    @Transactional
    fun deleteItem(id: Long) {
        val item = findById(id)
        item.deletedAt = LocalDateTime.now()
        itemRepository.save(item)
    }

    @Transactional(readOnly = true)
    fun findLowStock(categoryId: Long?): List<InventoryItemResponse> = itemRepository.findLowStock(categoryId)
        .map { InventoryItemResponse.from(it) }

    @Transactional
    fun createMovement(itemId: Long, request: CreateInventoryMovementRequest): InventoryMovementResponse {
        val item = findById(itemId)

        val delta = if (request.type == MovementType.ENTRY) request.quantity else -request.quantity

        val updatedRows = itemRepository.updateQuantityAtomically(itemId, delta)
        if (updatedRows == 0) {
            throw BadRequestException(
                "Insufficient stock. Current quantity: ${item.quantity}, requested: ${request.quantity}",
            )
        }

        // Re-read the actual quantity after the atomic update for accurate audit trail
        val actualNewQuantity = itemRepository.findCurrentQuantity(itemId)
        val previousQuantity = actualNewQuantity - delta

        val movement = InventoryMovement(
            item = item,
            movementType = request.type,
            quantity = request.quantity,
            previousQuantity = previousQuantity,
            newQuantity = actualNewQuantity,
            notes = request.notes,
        )

        val savedMovement = movementRepository.save(movement)
        val createdByUser = savedMovement.createdBy?.let { userRepository.findById(it).orElse(null) }
        return InventoryMovementResponse.from(savedMovement, createdByUser)
    }

    @Transactional(readOnly = true)
    fun getMovements(itemId: Long): List<InventoryMovementResponse> {
        findById(itemId) // validate item exists
        val movements = movementRepository.findByItemIdOrderByCreatedAtDesc(itemId)

        val userIds = movements.mapNotNull { it.createdBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return movements.map { movement ->
            val createdByUser = movement.createdBy?.let { usersById[it] }
            InventoryMovementResponse.from(movement, createdByUser)
        }
    }

    private fun validateTimeBased(
        pricingType: PricingType,
        timeUnit: com.insidehealthgt.hms.entity.TimeUnit?,
        timeInterval: Int?,
    ) {
        if (pricingType == PricingType.TIME_BASED) {
            if (timeUnit == null) {
                throw BadRequestException("Time unit is required for time-based pricing")
            }
            if (timeInterval == null || timeInterval <= 0) {
                throw BadRequestException("Time interval must be greater than 0 for time-based pricing")
            }
        }
    }

    private fun buildItemResponse(item: InventoryItem): InventoryItemResponse {
        val createdByUser = item.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = item.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return InventoryItemResponse.from(item, createdByUser, updatedByUser)
    }
}
