package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.request.CreateMedicationAdministrationRequest
import com.insidehealthgt.hms.dto.response.MedicationAdministrationResponse
import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.MedicationAdministration
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.MedicationAdministrationRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("ThrowsCount")
class MedicationAdministrationService(
    private val administrationRepository: MedicationAdministrationRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val inventoryItemService: InventoryItemService,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listAdministrations(
        admissionId: Long,
        orderId: Long,
        pageable: Pageable,
    ): Page<MedicationAdministrationResponse> {
        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException(
                "Medical order not found with id: $orderId for admission: $admissionId",
            )

        if (order.category != MedicalOrderCategory.MEDICAMENTOS) {
            throw BadRequestException("Medication administration is only available for MEDICAMENTOS orders")
        }

        val page = administrationRepository.findByOrderIdAndAdmissionId(orderId, admissionId, pageable)

        val userIds = page.content.mapNotNull { it.createdBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return page.map { admin ->
            val adminUser = admin.createdBy?.let { usersById[it] }
            MedicationAdministrationResponse.from(
                administration = admin,
                administeredByUser = adminUser,
                billable = admin.status == AdministrationStatus.GIVEN,
            )
        }
    }

    @Transactional
    fun createAdministration(
        admissionId: Long,
        orderId: Long,
        request: CreateMedicationAdministrationRequest,
    ): MedicationAdministrationResponse {
        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException(
                "Medical order not found with id: $orderId for admission: $admissionId",
            )

        // Verify order is for medications
        if (order.category != MedicalOrderCategory.MEDICAMENTOS) {
            throw BadRequestException("Medication administration is only available for MEDICAMENTOS orders")
        }

        // Verify admission is active
        if (!order.admission.isActive()) {
            throw BadRequestException("Cannot administer medication for a discharged admission")
        }

        // Verify order is active
        if (order.status == MedicalOrderStatus.DISCONTINUED) {
            throw BadRequestException("Cannot administer medication for a discontinued order")
        }

        // For GIVEN status, inventory item is required and we need to create EXIT movement
        val billable = if (request.status == AdministrationStatus.GIVEN) {
            val inventoryItem = order.inventoryItem
                ?: throw BadRequestException(
                    "Medical order must be linked to an inventory item before medication can be administered",
                )

            // Create EXIT movement — this publishes InventoryDispensedEvent which creates billing charge
            inventoryItemService.createMovement(
                inventoryItem.id!!,
                CreateInventoryMovementRequest(
                    type = MovementType.EXIT,
                    quantity = 1,
                    notes = "Medication administered - Order #${order.id}",
                    admissionId = admissionId,
                ),
            )
            true
        } else {
            false
        }

        val administration = MedicationAdministration(
            medicalOrder = order,
            admission = order.admission,
            status = request.status,
            notes = request.notes,
        )

        val saved = administrationRepository.save(administration)
        val adminUser = saved.createdBy?.let { userRepository.findById(it).orElse(null) }
        return MedicationAdministrationResponse.from(saved, adminUser, billable)
    }
}
