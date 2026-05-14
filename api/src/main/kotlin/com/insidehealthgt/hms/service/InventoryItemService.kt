package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryItemRequest
import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryItemRequest
import com.insidehealthgt.hms.dto.response.InventoryItemResponse
import com.insidehealthgt.hms.dto.response.InventoryMovementResponse
import com.insidehealthgt.hms.entity.InventoryCategory
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryCategoryRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Suppress("LongParameterList", "TooManyFunctions", "ThrowsCount")
class InventoryItemService(
    private val itemRepository: InventoryItemRepository,
    private val categoryRepository: InventoryCategoryRepository,
    private val lotRepository: InventoryLotRepository,
    private val medicationDetailsRepository: MedicationDetailsRepository,
    private val userRepository: UserRepository,
    private val movementService: InventoryMovementService,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): InventoryItem = itemRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(id)) }

    @Transactional(readOnly = true)
    fun findAll(
        categoryId: Long?,
        kind: InventoryKind?,
        search: String?,
        pageable: Pageable,
    ): Page<InventoryItemResponse> = itemRepository
        .findAllWithFilters(categoryId, kind, search?.trim() ?: "", pageable)
        .map { InventoryItemResponse.from(it) }

    @Transactional(readOnly = true)
    fun getItem(id: Long): InventoryItemResponse {
        val item = findById(id)
        return buildItemResponse(item)
    }

    @Transactional
    fun createItem(request: CreateInventoryItemRequest): InventoryItemResponse {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { BadRequestException(messageService.errorInventoryCategoryNotFound(request.categoryId)) }

        validateTimeBased(request.pricingType, request.timeUnit, request.timeInterval)
        validateKindLotTracking(request.kind, request.lotTrackingEnabled)
        validateDrugRequiresDetails(request.kind, hasDetails = false)
        validateCategoryKindMatch(category, request.kind)
        validateSku(request.sku, excludeItemId = null)

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
            kind = request.kind,
            sku = request.sku?.takeIf { it.isNotBlank() },
            lotTrackingEnabled = request.lotTrackingEnabled || request.kind == InventoryKind.DRUG,
        )

        val savedItem = itemRepository.save(item)
        return buildItemResponse(savedItem)
    }

    @Transactional
    @Suppress("CyclomaticComplexMethod")
    fun updateItem(id: Long, request: UpdateInventoryItemRequest): InventoryItemResponse {
        val item = findById(id)
        val oldKind = item.kind

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { BadRequestException(messageService.errorInventoryCategoryNotFound(request.categoryId)) }

        // Preserve current entity values when caller omits these fields — the
        // inventory edit form historically does not submit kind/sku/lotTracking,
        // and defaulting them to SUPPLY/null/false silently reclassified items.
        val effectiveKind = request.kind ?: item.kind
        val effectiveLotTracking = request.lotTrackingEnabled ?: item.lotTrackingEnabled
        val effectiveSku = when {
            request.sku == null -> item.sku
            request.sku.isBlank() -> null
            else -> request.sku
        }

        validateTimeBased(request.pricingType, request.timeUnit, request.timeInterval)
        validateKindLotTracking(effectiveKind, effectiveLotTracking)
        validateCategoryKindMatch(category, effectiveKind)
        validateSku(effectiveSku, excludeItemId = id)

        // Disabling lot tracking while non-deleted lots exist is rejected.
        val priorLotCount = lotRepository.countByItemIdAndDeletedAtIsNull(id)
        if (!effectiveLotTracking && effectiveKind != InventoryKind.DRUG && priorLotCount > 0) {
            throw BadRequestException(messageService.errorInventoryLotTrackingHasLots())
        }

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
        item.kind = effectiveKind
        item.sku = effectiveSku
        item.lotTrackingEnabled = effectiveLotTracking || effectiveKind == InventoryKind.DRUG

        // Reclassification consistency
        val details = medicationDetailsRepository.findByItemId(id)
        if (oldKind == InventoryKind.DRUG && effectiveKind != InventoryKind.DRUG && details != null) {
            // DRUG -> non-DRUG: soft-delete the details row in same tx
            details.deletedAt = LocalDateTime.now()
            medicationDetailsRepository.save(details)
        }
        if (effectiveKind == InventoryKind.DRUG && details == null) {
            throw BadRequestException(messageService.errorMedicationDetailsRequired())
        }

        val savedItem = itemRepository.save(item)
        return buildItemResponse(savedItem)
    }

    @Transactional
    fun deleteItem(id: Long) {
        val item = findById(id)
        val now = LocalDateTime.now()
        item.deletedAt = now
        // Cascade soft-delete to MedicationDetails (1:1 invariant — never orphan).
        medicationDetailsRepository.findByItemId(id)?.let {
            it.deletedAt = now
            medicationDetailsRepository.save(it)
        }
        itemRepository.save(item)
    }

    @Transactional(readOnly = true)
    fun findLowStock(categoryId: Long?): List<InventoryItemResponse> = itemRepository.findLowStock(categoryId)
        .map { InventoryItemResponse.from(it) }

    @Transactional
    fun createMovement(itemId: Long, request: CreateInventoryMovementRequest): InventoryMovementResponse =
        movementService.createMovement(itemId, request)

    @Transactional(readOnly = true)
    fun getMovements(itemId: Long): List<InventoryMovementResponse> = movementService.getMovements(itemId)

    /** Force-recompute the scalar quantity from active non-recalled lots; for ops drift recovery. */
    @Transactional
    fun reconcileQuantity(id: Long): InventoryItemResponse {
        val item = findById(id)
        if (item.lotTrackingEnabled) {
            itemRepository.recomputeQuantityFromLots(id)
        }
        return getItem(id)
    }

    private fun validateTimeBased(
        pricingType: PricingType,
        timeUnit: com.insidehealthgt.hms.entity.TimeUnit?,
        timeInterval: Int?,
    ) {
        if (pricingType == PricingType.TIME_BASED) {
            if (timeUnit == null) {
                throw BadRequestException(messageService.errorInventoryTimeUnitRequired())
            }
            if (timeInterval == null || timeInterval <= 0) {
                throw BadRequestException(messageService.errorInventoryTimeIntervalRequired())
            }
        }
    }

    private fun validateKindLotTracking(kind: InventoryKind, lotTrackingEnabled: Boolean) {
        val mustBeFalse = kind == InventoryKind.EQUIPMENT ||
            kind == InventoryKind.SERVICE ||
            kind == InventoryKind.PERSONNEL
        if (mustBeFalse && lotTrackingEnabled) {
            throw BadRequestException(messageService.errorInventoryLotTrackingNotAllowed(kind.name))
        }
        if (kind == InventoryKind.DRUG && !lotTrackingEnabled) {
            // Coerced upstream, but reject explicit attempts to disable.
            throw BadRequestException(messageService.errorInventoryDrugLotTrackingRequired())
        }
    }

    private fun validateDrugRequiresDetails(kind: InventoryKind, hasDetails: Boolean) {
        // Items created through the inventory endpoint cannot be kind=DRUG —
        // medications must be created through PharmacyController which atomically
        // creates the item + details. This guards against bypass.
        if (kind == InventoryKind.DRUG && !hasDetails) {
            throw BadRequestException(messageService.errorMedicationDetailsRequired())
        }
    }

    private fun validateCategoryKindMatch(category: InventoryCategory, kind: InventoryKind) {
        // Categories flagged as the system default for a given kind are
        // write-protected: only items of that kind may be filed there. Prevents
        // e.g. a SUPPLY landing under "Medicamentos" because someone picked the
        // wrong category from the general inventory form.
        val routed = category.defaultForKind ?: return
        if (kind != routed) {
            throw BadRequestException(messageService.errorInventoryCategoryKindMismatch(routed.name))
        }
    }

    private fun validateSku(sku: String?, excludeItemId: Long?) {
        if (sku.isNullOrBlank()) return
        val existing = itemRepository.findBySku(sku.trim())
        if (existing != null && existing.id != excludeItemId) {
            throw BadRequestException(messageService.errorInventorySkuExists(sku.trim()))
        }
    }

    private fun buildItemResponse(item: InventoryItem): InventoryItemResponse {
        val createdByUser = item.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = item.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return InventoryItemResponse.from(item, createdByUser, updatedByUser)
    }
}
