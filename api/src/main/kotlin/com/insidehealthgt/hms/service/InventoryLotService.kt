package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryLotRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryLotRequest
import com.insidehealthgt.hms.dto.response.InventoryLotResponse
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryLotUpsertDao
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InventoryLotService(
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val lotUpsertDao: InventoryLotUpsertDao,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun listByItem(itemId: Long): List<InventoryLotResponse> {
        itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        return lotRepository.findAllByItemIdOrderByExpirationDate(itemId).map { InventoryLotResponse.from(it) }
    }

    @Transactional
    fun createLot(itemId: Long, request: CreateInventoryLotRequest): InventoryLotResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        if (!item.lotTrackingEnabled) {
            throw BadRequestException(messageService.errorInventoryLotItemNotLotTracked())
        }

        // The explicit "create lot" endpoint must reject collisions on the
        // (itemId, lotNumber, expirationDate) identity instead of silently
        // adding to an existing lot's quantity. Going through `insertIfAbsent`
        // (DO NOTHING + RETURNING id) makes the duplicate check atomic with
        // the insert — a non-locking pre-check followed by upsertEntry would
        // race two concurrent create requests into a quantity-merge instead of
        // a 409. Stock additions to an existing lot still flow through the
        // ENTRY-movement path, which is the only caller of upsertEntry.
        val lotId = lotUpsertDao.insertIfAbsent(
            itemId = itemId,
            lotNumber = request.lotNumber,
            expirationDate = request.expirationDate,
            quantity = request.quantityOnHand,
            receivedAt = request.receivedAt,
            supplier = request.supplier,
        ) ?: throw ConflictException(messageService.errorInventoryLotDuplicate())

        val saved = lotRepository.findById(lotId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryLotNotFound(lotId)) }
        // notes is not part of the upsert key/payload — apply post-insert if supplied.
        if (request.notes != null && saved.notes != request.notes) {
            saved.notes = request.notes
            lotRepository.save(saved)
        }
        itemRepository.recomputeQuantityFromLots(itemId)
        return InventoryLotResponse.from(saved)
    }

    @Transactional
    fun updateLot(lotId: Long, request: UpdateInventoryLotRequest): InventoryLotResponse {
        val lot = lotRepository.findById(lotId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryLotNotFound(lotId)) }

        val identityChanged =
            lot.lotNumber != request.lotNumber || lot.expirationDate != request.expirationDate
        if (identityChanged) {
            lotRepository.findByItemIdAndLotNumberAndExpirationDate(
                lot.item.id!!,
                request.lotNumber,
                request.expirationDate,
            )?.let { collision ->
                if (collision.id != lotId) {
                    throw ConflictException(messageService.errorInventoryLotDuplicate())
                }
            }
        }

        lot.lotNumber = request.lotNumber
        lot.expirationDate = request.expirationDate
        lot.supplier = request.supplier
        lot.notes = request.notes
        lot.recalled = request.recalled
        lot.recalledReason = if (request.recalled) request.recalledReason else null
        val saved = lotRepository.save(lot)
        itemRepository.recomputeQuantityFromLots(lot.item.id!!)
        return InventoryLotResponse.from(saved)
    }

    @Transactional
    fun deleteLot(lotId: Long) {
        val lot = lotRepository.findById(lotId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryLotNotFound(lotId)) }
        if (lotRepository.existsMovementsByLotId(lotId) ||
            lotRepository.existsAdministrationsByLotId(lotId)
        ) {
            throw ConflictException(messageService.errorInventoryLotHasMovements())
        }
        lot.deletedAt = LocalDateTime.now()
        lotRepository.save(lot)
        itemRepository.recomputeQuantityFromLots(lot.item.id!!)
    }
}
