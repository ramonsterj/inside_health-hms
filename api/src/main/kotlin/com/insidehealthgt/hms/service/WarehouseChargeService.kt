package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateWarehouseChargeRequest
import com.insidehealthgt.hms.dto.response.WarehouseChargeResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.entity.WarehouseCharge
import com.insidehealthgt.hms.event.WarehouseChargeCreatedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.WarehouseChargeRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Charges a non-medical consumable from a warehouse to an admission (the "broken
 * towel to room 203" flow). Decrements warehouse stock (FEFO for lot-tracked
 * items), records an EXIT movement linked to the warehouse + admission, and emits
 * [WarehouseChargeCreatedEvent] so billing creates the [com.insidehealthgt.hms.entity.PatientCharge]
 * (AFTER_COMMIT). Audit row written in REQUIRES_NEW.
 */
@Service
@Suppress("LongParameterList")
class WarehouseChargeService(
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: InventoryItemRepository,
    private val admissionRepository: AdmissionRepository,
    private val chargeRepository: WarehouseChargeRepository,
    private val movementService: InventoryMovementService,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
    private val auditWriter: WarehouseAuditWriter,
    private val eventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {

    @Transactional
    fun createCharge(request: CreateWarehouseChargeRequest): WarehouseChargeResponse {
        val user = currentUserProvider.currentUserDetailsOrThrow()

        val warehouse = requireWarehouse(request.warehouseId)
        val item = requireItem(request.itemId)
        val admission = requireActiveAdmission(request.admissionId)

        scopeService.assertCanUseAsSource(user, warehouse)

        val quantity = BigDecimal(request.quantity)
        val exit = movementService.exitStock(
            item = item,
            warehouse = warehouse,
            requestedLotId = request.lotId,
            quantity = quantity,
            notes = "Warehouse charge: ${request.reason}",
            admission = admission,
            transfer = null,
        )

        val amount = item.price.multiply(quantity)
        val charge = chargeRepository.save(
            WarehouseCharge(
                warehouse = warehouse,
                item = item,
                lot = exit.lot,
                admission = admission,
                quantity = quantity,
                amount = amount,
                reason = request.reason,
                notes = request.notes,
            ),
        )

        eventPublisher.publishEvent(
            WarehouseChargeCreatedEvent(
                warehouseChargeId = charge.id!!,
                admissionId = admission.id!!,
                inventoryItemId = item.id!!,
                itemName = item.name,
                quantity = request.quantity,
                unitPrice = item.price,
                reason = request.reason,
            ),
        )

        auditWriter.writeCharge(
            userId = user.id,
            username = user.username,
            warehouseChargeId = charge.id!!,
            details = mapOf(
                "warehouse" to warehouse.code,
                "itemId" to item.id,
                "admissionId" to admission.id,
                "quantity" to request.quantity,
                "amount" to amount,
                "reason" to request.reason,
            ),
        )

        val createdByUser = charge.createdBy?.let { userRepository.findById(it).orElse(null) }
        return WarehouseChargeResponse.from(charge, createdByUser)
    }

    @Transactional(readOnly = true)
    fun listCharges(warehouseId: Long?, admissionId: Long?, pageable: Pageable): Page<WarehouseChargeResponse> {
        val page = chargeRepository.findHistory(warehouseId, admissionId, pageable)
        val userIds = page.content.mapNotNull { it.createdBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        return page.map { WarehouseChargeResponse.from(it, it.createdBy?.let { id -> usersById[id] }) }
    }

    private fun requireWarehouse(id: Long): Warehouse = warehouseRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorWarehouseNotFound(id)) }

    private fun requireItem(id: Long): InventoryItem = itemRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(id)) }

    private fun requireActiveAdmission(id: Long): Admission {
        val admission = admissionRepository.findByIdWithRelations(id)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(id))
        if (!admission.isActive()) {
            throw BadRequestException(messageService.errorBillingAdmissionNotActive())
        }
        return admission
    }
}
