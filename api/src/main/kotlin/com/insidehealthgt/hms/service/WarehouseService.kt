package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateWarehouseRequest
import com.insidehealthgt.hms.dto.request.UpdateWarehouseRequest
import com.insidehealthgt.hms.dto.response.WarehouseResponse
import com.insidehealthgt.hms.dto.response.WarehouseStockResponse
import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WarehouseService(
    private val warehouseRepository: WarehouseRepository,
    private val stockRepository: InventoryWarehouseStockRepository,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
    private val messageService: MessageService,
) {

    /** Warehouses visible to the caller (scoped by role / assignment). */
    @Transactional(readOnly = true)
    fun list(): List<WarehouseResponse> {
        val user = currentUserProvider.currentUserDetailsOrThrow()
        return scopeService.visibleWarehouses(user).map { WarehouseResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long): WarehouseResponse {
        val warehouse = findEntity(id)
        scopeService.assertCanView(currentUserProvider.currentUserDetailsOrThrow(), warehouse)
        return WarehouseResponse.from(warehouse)
    }

    @Transactional(readOnly = true)
    fun getStock(
        warehouseId: Long,
        search: String?,
        lowStockOnly: Boolean,
        pageable: Pageable,
    ): Page<WarehouseStockResponse> {
        val warehouse = findEntity(warehouseId)
        scopeService.assertCanView(currentUserProvider.currentUserDetailsOrThrow(), warehouse)
        return stockRepository
            .findStockForWarehouse(warehouseId, search?.trim() ?: "", lowStockOnly, pageable)
            .map { WarehouseStockResponse.from(it) }
    }

    @Transactional
    fun create(request: CreateWarehouseRequest): WarehouseResponse {
        if (warehouseRepository.existsByCode(request.code)) {
            throw ConflictException(messageService.errorWarehouseCodeExists(request.code))
        }
        val saved = warehouseRepository.save(
            Warehouse(
                code = request.code,
                name = request.name,
                description = request.description,
                active = request.active,
            ),
        )
        return WarehouseResponse.from(saved)
    }

    @Transactional
    fun update(id: Long, request: UpdateWarehouseRequest): WarehouseResponse {
        val warehouse = findEntity(id)
        warehouse.name = request.name
        warehouse.description = request.description
        warehouse.active = request.active
        return WarehouseResponse.from(warehouseRepository.save(warehouse))
    }

    @Transactional
    fun delete(id: Long) {
        val warehouse = findEntity(id)
        if (stockRepository.existsPositiveStockInWarehouse(id)) {
            throw ConflictException(messageService.errorWarehouseNotEmpty())
        }
        warehouse.deletedAt = LocalDateTime.now()
        warehouseRepository.save(warehouse)
    }

    private fun findEntity(id: Long): Warehouse = warehouseRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorWarehouseNotFound(id)) }
}
