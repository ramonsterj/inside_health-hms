package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryLotRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryLotRequest
import com.insidehealthgt.hms.dto.response.InventoryLotResponse
import com.insidehealthgt.hms.entity.WarehouseCodes
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryLotUpsertDao
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockUpsertDao
import com.insidehealthgt.hms.repository.WarehouseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Suppress("LongParameterList")
class InventoryLotService(
    private val itemRepository: InventoryItemRepository,
    private val lotRepository: InventoryLotRepository,
    private val lotUpsertDao: InventoryLotUpsertDao,
    private val stockRepository: InventoryWarehouseStockRepository,
    private val stockUpsertDao: InventoryWarehouseStockUpsertDao,
    private val warehouseRepository: WarehouseRepository,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun listByItem(itemId: Long): List<InventoryLotResponse> {
        itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        val lots = lotRepository.findAllByItemIdOrderByExpirationDate(itemId)
        val quantities = if (lots.isNotEmpty()) {
            stockRepository.sumByLotIds(lots.mapNotNull { it.id }).associate { it.lotId to it.quantity.toInt() }
        } else {
            emptyMap()
        }
        return lots.map { InventoryLotResponse.from(it, quantities[it.id] ?: 0) }
    }

    @Transactional
    @Suppress("ThrowsCount")
    fun createLot(itemId: Long, request: CreateInventoryLotRequest): InventoryLotResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        if (!item.lotTrackingEnabled) {
            throw BadRequestException(messageService.errorInventoryLotItemNotLotTracked())
        }

        // Reject collisions on the (itemId, lotNumber, expirationDate) identity
        // atomically (DO NOTHING + RETURNING id). Stock for the lot lands in the
        // ADMINISTRACION receiving warehouse — pharmacist later transfers it out.
        val lotId = lotUpsertDao.insertIfAbsent(
            itemId = itemId,
            lotNumber = request.lotNumber,
            expirationDate = request.expirationDate,
            receivedAt = request.receivedAt,
            supplier = request.supplier,
        ) ?: throw ConflictException(messageService.errorInventoryLotDuplicate())

        val saved = lotRepository.findById(lotId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryLotNotFound(lotId)) }
        if (request.notes != null && saved.notes != request.notes) {
            saved.notes = request.notes
            lotRepository.save(saved)
        }

        if (request.quantityOnHand > 0) {
            val receiving = warehouseRepository.findByCode(WarehouseCodes.RECEIVING)
                ?: throw BadRequestException(messageService.errorWarehouseInactive(WarehouseCodes.RECEIVING))
            stockUpsertDao.upsertAdd(itemId, receiving.id!!, lotId, BigDecimal(request.quantityOnHand))
        }

        return InventoryLotResponse.from(saved, stockRepository.sumByLot(lotId).toInt())
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
        return InventoryLotResponse.from(saved, stockRepository.sumByLot(lotId).toInt())
    }

    @Transactional
    fun deleteLot(lotId: Long) {
        val lot = lotRepository.findById(lotId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryLotNotFound(lotId)) }
        if (lotRepository.existsMovementsByLotId(lotId) ||
            lotRepository.existsAdministrationsByLotId(lotId) ||
            lotRepository.existsWarehouseStockByLotId(lotId)
        ) {
            throw ConflictException(messageService.errorInventoryLotHasMovements())
        }
        lot.deletedAt = LocalDateTime.now()
        lotRepository.save(lot)
    }
}
