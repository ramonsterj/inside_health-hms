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
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val lotRepository: InventoryLotRepository,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun listMedications(
        section: MedicationSection?,
        controlled: Boolean?,
        search: String?,
        pageable: Pageable,
    ): Page<MedicationResponse> = detailsRepository.findAllWithFilters(
        section,
        controlled,
        search?.trim() ?: "",
        pageable,
    ).map { MedicationResponse.from(it.item, it) }

    @Transactional(readOnly = true)
    fun getMedication(itemId: Long): MedicationResponse {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        val details = detailsRepository.findByItemId(itemId)
            ?: throw ResourceNotFoundException(messageService.errorMedicationDetailsNotFound(itemId))
        return MedicationResponse.from(item, details)
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

        return MedicationResponse.from(item, details)
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
     * Non-binding FEFO preview: returns the lot the next dispense would select,
     * or null when no eligible lot exists. UI-only — the write path locks and
     * debits via [InventoryLotRepository.findNextFefoLotForUpdate].
     */
    @Transactional(readOnly = true)
    fun fefoPreview(itemId: Long, quantity: Int): InventoryLotResponse? =
        lotRepository.peekFefoCandidates(itemId, quantity)
            .firstOrNull()
            ?.let(InventoryLotResponse::from)
}
