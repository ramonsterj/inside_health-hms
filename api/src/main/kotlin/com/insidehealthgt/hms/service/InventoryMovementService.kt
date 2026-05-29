package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.response.InventoryMovementResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryLot
import com.insidehealthgt.hms.entity.InventoryMovement
import com.insidehealthgt.hms.entity.InventoryTransfer
import com.insidehealthgt.hms.entity.InventoryWarehouseStock
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.entity.WarehouseCodes
import com.insidehealthgt.hms.event.InventoryDispensedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnprocessableEntityException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryLotUpsertDao
import com.insidehealthgt.hms.repository.InventoryMovementRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockUpsertDao
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Centralizes warehouse-scoped stock movement handling. Every EXIT/ENTRY now
 * mutates a row in `inventory_warehouse_stock` (the per-warehouse on-hand) rather
 * than the dropped global `inventory_items.quantity` / `inventory_lots.quantity_on_hand`
 * columns.
 *
 * Two branches:
 *   - Scalar (item.lotTrackingEnabled = false): one stock row per (item, warehouse),
 *     lot_id NULL.
 *   - Lot-tracked: FEFO selection on EXIT (warehouse-scoped, AC-5), find-or-create
 *     lot identity + upsert stock row on ENTRY.
 *
 * [exitStock] / [entryStock] are the reusable primitives the transfer and charge
 * services call within their own transactions; [createMovement] is the public
 * single-warehouse entry/exit endpoint.
 */
@Service
@Suppress("LongParameterList", "ThrowsCount", "TooManyFunctions")
class InventoryMovementService(
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val lotUpsertDao: InventoryLotUpsertDao,
    private val stockRepository: InventoryWarehouseStockRepository,
    private val stockUpsertDao: InventoryWarehouseStockUpsertDao,
    private val movementRepository: InventoryMovementRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val warehouseRepository: WarehouseRepository,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
    private val eventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
) {

    /** Outcome of an EXIT: the saved movement and the lot it debited (null for scalar items). */
    data class ExitResult(val movement: InventoryMovement, val lot: InventoryLot?)

    @Transactional
    fun createMovement(itemId: Long, request: CreateInventoryMovementRequest): InventoryMovementResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }

        val admission = request.admissionId?.let { admissionId ->
            admissionRepository.findById(admissionId)
                .orElseThrow { BadRequestException(messageService.errorInventoryAdmissionNotFound(admissionId)) }
        }

        val warehouse = resolveWarehouse(request)

        val movement = when (request.type) {
            MovementType.EXIT -> exitStock(
                item = item,
                warehouse = warehouse,
                requestedLotId = request.lotId,
                quantity = BigDecimal(request.quantity),
                notes = request.notes,
                admission = admission,
                transfer = null,
            ).movement

            MovementType.ENTRY -> {
                val lot = resolveOrCreateEntryLot(item, request)
                entryStock(
                    item = item,
                    warehouse = warehouse,
                    lot = lot,
                    quantity = BigDecimal(request.quantity),
                    notes = request.notes,
                    admission = admission,
                    transfer = null,
                )
            }
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

    /**
     * Debit [quantity] from the item's stock in [warehouse]. Lot-tracked items use
     * warehouse-scoped FEFO unless [requestedLotId] is given. Throws 422
     * `error.warehouse.out-of-stock` (naming the warehouse + item) when the
     * warehouse cannot cover the request — no rows are written. Reusable by the
     * transfer and charge services within their own transaction.
     */
    @Suppress("LongParameterList")
    fun exitStock(
        item: InventoryItem,
        warehouse: Warehouse,
        requestedLotId: Long?,
        quantity: BigDecimal,
        notes: String?,
        admission: Admission?,
        transfer: InventoryTransfer?,
    ): ExitResult {
        val previousTotal = warehouseStockSum(item.id!!, warehouse.id!!)

        val (stockRow, lot) = if (item.lotTrackingEnabled) {
            selectLotStockForExit(item, warehouse, requestedLotId, quantity)
        } else {
            val row = stockRepository.findForUpdate(item.id!!, warehouse.id!!, null)
            if (row == null || row.quantity < quantity) {
                throw outOfStock(warehouse, item)
            }
            row to null
        }

        stockRow.quantity = stockRow.quantity.subtract(quantity)
        stockRepository.save(stockRow)

        val newTotal = warehouseStockSum(item.id!!, warehouse.id!!)
        val movement = movementRepository.save(
            InventoryMovement(
                item = item,
                movementType = MovementType.EXIT,
                quantity = quantity.toInt(),
                previousQuantity = previousTotal,
                newQuantity = newTotal,
                notes = notes,
                admission = admission,
                lot = lot,
                warehouse = warehouse,
                transfer = transfer,
            ),
        )
        return ExitResult(movement, lot)
    }

    /**
     * Credit [quantity] to the item's stock in [warehouse] for the given lot
     * (null for scalar items), creating the stock row if absent. Reusable by the
     * transfer service.
     */
    @Suppress("LongParameterList")
    fun entryStock(
        item: InventoryItem,
        warehouse: Warehouse,
        lot: InventoryLot?,
        quantity: BigDecimal,
        notes: String?,
        admission: Admission?,
        transfer: InventoryTransfer?,
    ): InventoryMovement {
        val previousTotal = warehouseStockSum(item.id!!, warehouse.id!!)

        stockUpsertDao.upsertAdd(item.id!!, warehouse.id!!, lot?.id, quantity)

        val newTotal = warehouseStockSum(item.id!!, warehouse.id!!)
        return movementRepository.save(
            InventoryMovement(
                item = item,
                movementType = MovementType.ENTRY,
                quantity = quantity.toInt(),
                previousQuantity = previousTotal,
                newQuantity = newTotal,
                notes = notes,
                admission = admission,
                lot = lot,
                warehouse = warehouse,
                transfer = transfer,
            ),
        )
    }

    private fun selectLotStockForExit(
        item: InventoryItem,
        warehouse: Warehouse,
        requestedLotId: Long?,
        quantity: BigDecimal,
    ): Pair<InventoryWarehouseStock, InventoryLot> {
        if (requestedLotId != null) {
            val lot = lotRepository.findById(requestedLotId).orElse(null)
                ?: throw BadRequestException(messageService.errorInventoryLotNotFound(requestedLotId))
            if (lot.item.id != item.id) {
                throw BadRequestException(messageService.errorInventoryLotItemMismatch())
            }
            if (lot.recalled) {
                throw BadRequestException(messageService.errorInventoryLotRecalled())
            }
            val row = stockRepository.findForUpdate(item.id!!, warehouse.id!!, requestedLotId)
            if (row == null || row.quantity < quantity) {
                throw outOfStock(warehouse, item)
            }
            return row to lot
        }
        val row = stockRepository.findFefoForUpdate(item.id!!, warehouse.id!!, quantity).firstOrNull()
            ?: throw outOfStock(warehouse, item)
        return row to row.lot!!
    }

    private fun resolveOrCreateEntryLot(item: InventoryItem, request: CreateInventoryMovementRequest): InventoryLot? {
        if (!item.lotTrackingEnabled) return null
        val expirationDate = request.expirationDate
            ?: throw BadRequestException(messageService.errorInventoryLotExpirationRequired())
        val lotId = lotUpsertDao.upsertEntry(
            itemId = item.id!!,
            lotNumber = request.lotNumber,
            expirationDate = expirationDate,
            receivedAt = LocalDate.now(),
            supplier = request.supplier,
        )
        return lotRepository.findById(lotId)
            .orElseThrow { BadRequestException(messageService.errorInventoryLotNotFound(lotId)) }
    }

    private fun resolveWarehouse(request: CreateInventoryMovementRequest): Warehouse {
        request.warehouseId?.let { id ->
            return warehouseRepository.findById(id)
                .orElseThrow { BadRequestException(messageService.errorWarehouseNotFound(id)) }
        }
        return when (request.type) {
            MovementType.EXIT ->
                scopeService.resolveDispensingWarehouse(currentUserProvider.currentUserDetailsOrThrow())

            MovementType.ENTRY ->
                warehouseRepository.findByCode(WarehouseCodes.RECEIVING)
                    ?: throw BadRequestException(messageService.errorWarehouseInactive(WarehouseCodes.RECEIVING))
        }
    }

    private fun warehouseStockSum(itemId: Long, warehouseId: Long): Int =
        stockRepository.sumByItemAndWarehouse(itemId, warehouseId).toInt()

    private fun outOfStock(warehouse: Warehouse, item: InventoryItem) =
        UnprocessableEntityException(messageService.errorWarehouseOutOfStock(warehouse.name, item.name))
}
