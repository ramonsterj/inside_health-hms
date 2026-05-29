package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateMedicationRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicationRequest
import com.insidehealthgt.hms.dto.response.InventoryLotResponse
import com.insidehealthgt.hms.dto.response.MedicationResponse
import com.insidehealthgt.hms.entity.InventoryCategory
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.entity.MedicationDetails
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryCategoryRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Read-model + write-model service that composes InventoryItem + MedicationDetails
 * so callers do not have to know about the two-table layout.
 */
@Service
@Suppress("LongParameterList")
class PharmacyService(
    private val itemRepository: InventoryItemRepository,
    private val detailsRepository: MedicationDetailsRepository,
    private val categoryRepository: InventoryCategoryRepository,
    private val medicationDetailsService: MedicationDetailsService,
    private val stockRepository: InventoryWarehouseStockRepository,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun listMedications(
        section: MedicationSection?,
        controlled: Boolean?,
        search: String?,
        pageable: Pageable,
    ): Page<MedicationResponse> {
        val page = detailsRepository.findAllWithFilters(section, controlled, search?.trim() ?: "", pageable)
        val itemIds = page.content.mapNotNull { it.item.id }
        val quantities = if (itemIds.isNotEmpty()) {
            stockRepository.sumByItemIds(itemIds).associate { it.itemId to it.quantity.toInt() }
        } else {
            emptyMap()
        }
        return page.map { MedicationResponse.from(it.item, it, quantities[it.item.id] ?: 0) }
    }

    @Transactional(readOnly = true)
    fun getMedication(itemId: Long): MedicationResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        val details = detailsRepository.findByItemId(itemId)
            ?: throw ResourceNotFoundException(messageService.errorMedicationDetailsNotFound(itemId))
        val breakdown = stockRepository.findWarehouseBreakdownForItem(itemId).map { row ->
            MedicationResponse.WarehouseStockBreakdown(
                warehouseId = row.warehouseId,
                warehouseCode = row.code,
                warehouseName = row.name,
                quantity = row.quantity.toInt(),
            )
        }
        return MedicationResponse.from(item, details, stockRepository.sumByItem(itemId).toInt(), breakdown)
    }

    @Transactional
    fun createMedication(request: CreateMedicationRequest): MedicationResponse {
        val category = resolveDrugCategory()

        // SKU uniqueness.
        request.sku?.takeIf { it.isNotBlank() }?.let { sku ->
            itemRepository.findBySku(sku)?.let {
                throw BadRequestException(messageService.errorInventorySkuExists(sku))
            }
        }

        val item = itemRepository.save(
            InventoryItem(
                category = category,
                name = request.name,
                description = request.description,
                price = request.price,
                cost = request.cost,
                restockLevel = request.restockLevel,
                pricingType = PricingType.FLAT,
                timeUnit = null,
                timeInterval = null,
                active = request.active,
                kind = InventoryKind.DRUG,
                sku = request.sku?.takeIf { it.isNotBlank() },
                lotTrackingEnabled = true,
            ),
        )

        val details = detailsRepository.save(
            MedicationDetails(
                item = item,
                genericName = request.genericName,
                commercialName = request.commercialName,
                strength = request.strength,
                dosageForm = request.dosageForm,
                route = request.route,
                controlled = request.controlled,
                atcCode = request.atcCode,
                section = request.section,
                reviewStatus = MedicationReviewStatus.CONFIRMED,
            ),
        )

        // Freshly created medication has no warehouse stock yet — quantity is 0.
        return MedicationResponse.from(item, details, quantity = 0)
    }

    @Transactional
    fun updateMedication(itemId: Long, request: UpdateMedicationRequest): MedicationResponse =
        medicationDetailsService.update(itemId, request)

    private fun resolveDrugCategory(): InventoryCategory =
        categoryRepository.findByDefaultForKindAndActiveTrue(InventoryKind.DRUG)
            ?: throw BadRequestException(
                messageService.errorInventoryCategoryDefaultMissing(InventoryKind.DRUG.name),
            )

    /**
     * Non-binding, warehouse-scoped FEFO preview: returns the lot the next
     * dispense from the resolved warehouse would select (or [warehouseId] when
     * given), or null when no eligible lot exists there. UI-only — the write path
     * locks and debits via [InventoryWarehouseStockRepository.findFefoForUpdate].
     */
    @Transactional(readOnly = true)
    fun fefoPreview(itemId: Long, quantity: Int, warehouseId: Long? = null): InventoryLotResponse? {
        val user = currentUserProvider.currentUserDetailsOrThrow()
        val resolvedWarehouseId = if (warehouseId != null) {
            scopeService.assertCanView(user, warehouseId)
            warehouseId
        } else {
            scopeService.resolveDispensingWarehouse(user).id!!
        }
        return stockRepository
            .peekFefoForWarehouse(itemId, resolvedWarehouseId, BigDecimal(quantity))
            .firstOrNull()
            ?.let { row -> row.lot?.let { InventoryLotResponse.from(it, row.quantity.toInt()) } }
    }
}
