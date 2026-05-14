package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.response.InventoryMovementResponse
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryLot
import com.insidehealthgt.hms.entity.InventoryMovement
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.event.InventoryDispensedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryLotUpsertDao
import com.insidehealthgt.hms.repository.InventoryMovementRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Centralizes stock movement handling. Has two branches:
 *   - Scalar branch (item.lotTrackingEnabled = false): the legacy atomic-update path.
 *   - Lot branch (item.lotTrackingEnabled = true): FEFO selection on EXIT, ON CONFLICT
 *     upsert on ENTRY, then `recomputeQuantityFromLots` to keep the scalar `quantity`
 *     column consistent with the SUM of active non-recalled lots.
 *
 * Extracted from InventoryItemService so the FEFO branch has a clean home.
 */
@Service
@Suppress("LongParameterList", "ThrowsCount", "ComplexMethod")
class InventoryMovementService(
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val lotUpsertDao: InventoryLotUpsertDao,
    private val movementRepository: InventoryMovementRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
) {

    @Transactional
    fun createMovement(itemId: Long, request: CreateInventoryMovementRequest): InventoryMovementResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }

        val admission = request.admissionId?.let { admissionId ->
            admissionRepository.findById(admissionId)
                .orElseThrow { BadRequestException(messageService.errorInventoryAdmissionNotFound(admissionId)) }
        }

        val movement = if (item.lotTrackingEnabled) {
            handleLotTrackedMovement(item, request, admission)
        } else {
            handleScalarMovement(item, request, admission)
        }

        if (movement.movementType == MovementType.EXIT && admission != null) {
            eventPublisher.publishEvent(
                InventoryDispensedEvent(
                    admissionId = admission.id!!,
                    inventoryItemId = item.id!!,
                    itemName = item.name,
                    quantity = movement.quantity,
                    unitPrice = item.price,
                    lotId = movement.lot?.id,
                ),
            )
        }

        val createdByUser = movement.createdBy?.let { userRepository.findById(it).orElse(null) }
        return InventoryMovementResponse.from(movement, createdByUser)
    }

    @Transactional(readOnly = true)
    fun getMovements(itemId: Long): List<InventoryMovementResponse> {
        itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        val movements = movementRepository.findByItemIdOrderByCreatedAtDesc(itemId)
        val userIds = movements.mapNotNull { it.createdBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        return movements.map { m -> InventoryMovementResponse.from(m, m.createdBy?.let { usersById[it] }) }
    }

    private fun handleScalarMovement(
        item: InventoryItem,
        request: CreateInventoryMovementRequest,
        admission: com.insidehealthgt.hms.entity.Admission?,
    ): InventoryMovement {
        val delta = if (request.type == MovementType.ENTRY) request.quantity else -request.quantity

        val updatedRows = itemRepository.updateQuantityAtomically(item.id!!, delta)
        if (updatedRows == 0) {
            throw BadRequestException(
                messageService.errorInventoryInsufficientStock(item.quantity, request.quantity),
            )
        }
        val actualNewQuantity = itemRepository.findCurrentQuantity(item.id!!)
        val previousQuantity = actualNewQuantity - delta

        val movement = InventoryMovement(
            item = item,
            movementType = request.type,
            quantity = request.quantity,
            previousQuantity = previousQuantity,
            newQuantity = actualNewQuantity,
            notes = request.notes,
            admission = admission,
            lot = null,
        )
        return movementRepository.save(movement)
    }

    private fun handleLotTrackedMovement(
        item: InventoryItem,
        request: CreateInventoryMovementRequest,
        admission: com.insidehealthgt.hms.entity.Admission?,
    ): InventoryMovement = when (request.type) {
        MovementType.EXIT -> handleLotExit(item, request, admission)
        MovementType.ENTRY -> handleLotEntry(item, request, admission)
    }

    private fun handleLotExit(
        item: InventoryItem,
        request: CreateInventoryMovementRequest,
        admission: com.insidehealthgt.hms.entity.Admission?,
    ): InventoryMovement {
        val previousTotal = item.quantity

        val selectedLot: InventoryLot = if (request.lotId != null) {
            // Manual admin override: lock the chosen lot row before reading
            // quantity_on_hand. Otherwise two concurrent overrides on the same
            // lot could both pass the quantity check and over-debit. Spec § R-FEFO.
            val lot = lotRepository.findByIdForUpdate(request.lotId)
                ?: throw BadRequestException(messageService.errorInventoryLotNotFound(request.lotId))
            if (lot.item.id != item.id) {
                throw BadRequestException(messageService.errorInventoryLotItemMismatch())
            }
            if (lot.recalled) {
                throw BadRequestException(messageService.errorInventoryLotRecalled())
            }
            if (lot.quantityOnHand < request.quantity) {
                throw BadRequestException(
                    messageService.errorInventoryInsufficientStock(lot.quantityOnHand, request.quantity),
                )
            }
            lot
        } else {
            val candidates = lotRepository.findFefoCandidates(item.id!!, request.quantity)
            candidates.firstOrNull()
                ?: throw BadRequestException(messageService.errorMedicationFefoNoSingleLot())
        }

        selectedLot.quantityOnHand -= request.quantity
        lotRepository.save(selectedLot)

        itemRepository.recomputeQuantityFromLots(item.id!!)
        val newTotal = itemRepository.findCurrentQuantity(item.id!!)

        val movement = InventoryMovement(
            item = item,
            movementType = MovementType.EXIT,
            quantity = request.quantity,
            previousQuantity = previousTotal,
            newQuantity = newTotal,
            notes = request.notes,
            admission = admission,
            lot = selectedLot,
        )
        return movementRepository.save(movement)
    }

    private fun handleLotEntry(
        item: InventoryItem,
        request: CreateInventoryMovementRequest,
        admission: com.insidehealthgt.hms.entity.Admission?,
    ): InventoryMovement {
        val expirationDate = request.expirationDate
            ?: throw BadRequestException(messageService.errorInventoryLotExpirationRequired())

        val previousTotal = item.quantity

        // Race-safe upsert against the partial unique index — two concurrent
        // ENTRY movements for the same (item, lotNumber, expirationDate) can no
        // longer lose updates the way a find-then-save would.
        val lotId = lotUpsertDao.upsertEntry(
            itemId = item.id!!,
            lotNumber = request.lotNumber,
            expirationDate = expirationDate,
            quantity = request.quantity,
            receivedAt = LocalDate.now(),
            supplier = request.supplier,
        )
        val lot = lotRepository.findById(lotId).orElseThrow {
            BadRequestException(messageService.errorInventoryLotNotFound(lotId))
        }

        itemRepository.recomputeQuantityFromLots(item.id!!)
        val newTotal = itemRepository.findCurrentQuantity(item.id!!)

        val movement = InventoryMovement(
            item = item,
            movementType = MovementType.ENTRY,
            quantity = request.quantity,
            previousQuantity = previousTotal,
            newQuantity = newTotal,
            notes = request.notes,
            admission = admission,
            lot = lot,
        )
        return movementRepository.save(movement)
    }
}
