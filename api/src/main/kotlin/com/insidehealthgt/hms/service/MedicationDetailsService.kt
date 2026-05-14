package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.UpdateMedicationRequest
import com.insidehealthgt.hms.dto.response.MedicationResponse
import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CRUD for MedicationDetails. Saving a row whose reviewStatus is NEEDS_REVIEW
 * through the normal edit path auto-transitions it to CONFIRMED and clears notes (AC-18).
 */
@Service
class MedicationDetailsService(
    private val itemRepository: InventoryItemRepository,
    private val detailsRepository: MedicationDetailsRepository,
    private val messageService: MessageService,
) {

    @Transactional
    fun update(itemId: Long, request: UpdateMedicationRequest): MedicationResponse {
        val item = loadDrugItem(itemId)
        val details = detailsRepository.findByItemId(itemId)
            ?: throw ResourceNotFoundException(messageService.errorMedicationDetailsNotFound(itemId))

        validateSkuUnique(item, request.sku)

        // Update item-side (category is invariant — re-shelf via the generic
        // inventory edit screen if needed; the pharmacy form doesn't touch it).
        item.name = request.name
        item.description = request.description
        item.price = request.price
        item.cost = request.cost
        item.sku = request.sku?.takeIf { it.isNotBlank() }
        item.restockLevel = request.restockLevel
        item.active = request.active
        itemRepository.save(item)

        // Update details
        details.genericName = request.genericName
        details.commercialName = request.commercialName
        details.strength = request.strength
        details.dosageForm = request.dosageForm
        details.route = request.route
        details.controlled = request.controlled
        details.atcCode = request.atcCode
        details.section = request.section
        if (details.reviewStatus == MedicationReviewStatus.NEEDS_REVIEW) {
            details.reviewStatus = MedicationReviewStatus.CONFIRMED
            details.reviewNotes = null
        }
        detailsRepository.save(details)

        return MedicationResponse.from(item, details)
    }

    private fun loadDrugItem(itemId: Long): com.insidehealthgt.hms.entity.InventoryItem {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException(messageService.errorInventoryItemNotFound(itemId)) }
        if (item.kind != InventoryKind.DRUG) {
            throw BadRequestException(messageService.errorMedicationDetailsNotDrug())
        }
        return item
    }

    private fun validateSkuUnique(item: com.insidehealthgt.hms.entity.InventoryItem, requestedSku: String?) {
        val newSku = requestedSku?.takeIf { it.isNotBlank() && it != item.sku } ?: return
        val collision = itemRepository.findBySku(newSku)
        if (collision != null && collision.id != item.id) {
            throw BadRequestException(messageService.errorInventorySkuExists(newSku))
        }
    }
}
