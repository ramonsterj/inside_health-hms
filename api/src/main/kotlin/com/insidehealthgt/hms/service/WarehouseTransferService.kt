package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateTransferRequest
import com.insidehealthgt.hms.dto.response.TransferResponse
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryLot
import com.insidehealthgt.hms.entity.InventoryTransfer
import com.insidehealthgt.hms.entity.TransferStatus
import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryTransferRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Inter-warehouse transfers.
 *
 * ## Atomicity rules
 *
 * [createTransfer] runs in a single transaction. It writes one
 * `inventory_transfers` row plus two `inventory_movements` (an EXIT on the source
 * and an ENTRY on the destination). The source debit goes through
 * [InventoryMovementService.exitStock], which takes a `SELECT … FOR UPDATE` on the
 * source `inventory_warehouse_stock` row before validating and decrementing — so
 * concurrent transfers / dispenses / charges on the same row serialize and cannot
 * oversell (NFR concurrency, AC-16). If the source cannot cover the request the
 * debit throws 422 and the whole transaction (including the transfer row) rolls
 * back, leaving no partial state (AC-7).
 *
 * The destination credit upserts (`ON CONFLICT DO UPDATE`) so the destination
 * stock row is created if absent (AC-6). The audit row is written separately in a
 * REQUIRES_NEW transaction so an audit failure cannot drop the stock change.
 *
 * v1 ships one-step transfers: the row is created already `COMPLETED`.
 */
@Service
@Suppress("LongParameterList")
class WarehouseTransferService(
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val transferRepository: InventoryTransferRepository,
    private val movementService: InventoryMovementService,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
    private val auditWriter: WarehouseAuditWriter,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {

    @Transactional
    fun createTransfer(request: CreateTransferRequest): TransferResponse {
        val user = currentUserProvider.currentUserDetailsOrThrow()

        if (request.sourceWarehouseId == request.destinationWarehouseId) {
            throw BadRequestException(messageService.errorWarehouseTransferSameWarehouse())
        }

        val source = requireWarehouse(request.sourceWarehouseId)
        val destination = requireWarehouse(request.destinationWarehouseId)
        val item = requireItem(request.itemId)

        scopeService.assertCanUseAsSource(user, source)

        val lot = resolveLot(item, request.lotId)
        val quantity = BigDecimal(request.quantity)
        val now = LocalDateTime.now()

        val transfer = transferRepository.save(
            InventoryTransfer(
                sourceWarehouse = source,
                destinationWarehouse = destination,
                item = item,
                lot = lot,
                quantity = quantity,
                status = TransferStatus.COMPLETED,
                notes = request.notes,
                issuedAt = now,
                issuedBy = user.id,
                completedAt = now,
                completedBy = user.id,
            ),
        )

        // Source debit first — throws 422 (rolling back the transfer row) if the
        // source warehouse cannot cover the request.
        movementService.exitStock(
            item = item,
            warehouse = source,
            requestedLotId = lot?.id,
            quantity = quantity,
            notes = transferNote("Transfer out", destination),
            admission = null,
            transfer = transfer,
        )
        movementService.entryStock(
            item = item,
            warehouse = destination,
            lot = lot,
            quantity = quantity,
            notes = transferNote("Transfer in", source),
            admission = null,
            transfer = transfer,
        )

        auditWriter.writeTransfer(
            userId = user.id,
            username = user.username,
            transferId = transfer.id!!,
            details = mapOf(
                "source" to source.code,
                "destination" to destination.code,
                "itemId" to item.id,
                "lotId" to lot?.id,
                "quantity" to request.quantity,
            ),
        )

        val issuedByUser = userRepository.findById(user.id).orElse(null)
        return TransferResponse.from(transfer, issuedByUser)
    }

    @Transactional(readOnly = true)
    fun listTransfers(warehouseId: Long?, itemId: Long?, pageable: Pageable): Page<TransferResponse> {
        val page = transferRepository.findHistory(warehouseId, itemId, pageable)
        val userIds = page.content.map { it.issuedBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        return page.map { TransferResponse.from(it, usersById[it.issuedBy]) }
    }

    @Suppress("ThrowsCount")
    private fun resolveLot(item: InventoryItem, lotId: Long?): InventoryLot? {
        if (!item.lotTrackingEnabled) return null
        val id = lotId ?: throw BadRequestException(messageService.errorWarehouseLotRequired())
        val lot = lotRepository.findById(id).orElse(null)
            ?: throw BadRequestException(messageService.errorInventoryLotNotFound(id))
        if (lot.item.id != item.id) {
            throw BadRequestException(messageService.errorInventoryLotItemMismatch())
        }
        if (lot.recalled) {
            throw BadRequestException(messageService.errorInventoryLotRecalled())
        }
        return lot
    }

    private fun requireWarehouse(id: Long): Warehouse = warehouseRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorWarehouseNotFound(id)) }

    private fun requireItem(id: Long): InventoryItem = itemRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(id)) }

    private fun transferNote(prefix: String, other: Warehouse): String = "$prefix (${other.code})"
}
