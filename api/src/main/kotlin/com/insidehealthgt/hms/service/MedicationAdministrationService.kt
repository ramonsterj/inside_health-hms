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
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.MedicationAdministrationRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("ThrowsCount", "LongParameterList")
class MedicationAdministrationService(
    private val administrationRepository: MedicationAdministrationRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val inventoryItemService: InventoryItemService,
    private val lotRepository: InventoryLotRepository,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun listAdministrations(
        admissionId: Long,
        orderId: Long,
        pageable: Pageable,
    ): Page<MedicationAdministrationResponse> {
        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException(
                messageService.errorMedicationOrderNotFound(orderId, admissionId),
            )

        if (order.category != MedicalOrderCategory.MEDICAMENTOS) {
            throw BadRequestException(messageService.errorMedicationOnlyMedicamentos())
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
                messageService.errorMedicationOrderNotFound(orderId, admissionId),
            )

        // Verify order is for medications
        if (order.category != MedicalOrderCategory.MEDICAMENTOS) {
            throw BadRequestException(messageService.errorMedicationOnlyMedicamentos())
        }

        // Verify admission is active
        if (!order.admission.isActive()) {
            throw BadRequestException(messageService.errorMedicationAdmissionDischarged())
        }

        // Verify order is active (not discontinued / rejected / pending authorization)
        if (order.status != MedicalOrderStatus.AUTORIZADO) {
            throw BadRequestException(messageService.errorMedicationOrderDiscontinued())
        }

        // Admin-only override: nurses sending lotId receive 403.
        if (request.lotId != null) {
            val ud = currentUserProvider.currentUserDetails()
            val isAdmin = ud?.hasRole("ADMIN") == true || ud?.hasPermission("inventory-lot:update") == true
            if (!isAdmin) {
                throw ForbiddenException(messageService.errorMedicationLotOverrideForbidden())
            }
        }

        // For GIVEN status, inventory item is required and we need to create EXIT movement
        val (billable, lotId) = if (request.status == AdministrationStatus.GIVEN) {
            val inventoryItem = order.inventoryItem
                ?: throw BadRequestException(
                    messageService.errorMedicationOrderNoInventory(),
                )

            if (request.quantity < 1) {
                throw BadRequestException(messageService.errorMedicationQuantityRequired())
            }

            // Create EXIT movement — this publishes InventoryDispensedEvent which creates billing charge
            val movement = inventoryItemService.createMovement(
                inventoryItem.id!!,
                CreateInventoryMovementRequest(
                    type = MovementType.EXIT,
                    quantity = request.quantity,
                    notes = "Medication administered - Order #${order.id}",
                    admissionId = admissionId,
                    lotId = request.lotId,
                ),
            )
            true to movement.lotId
        } else {
            false to null
        }

        val lot = lotId?.let { lotRepository.findById(it).orElse(null) }

        val administration = MedicationAdministration(
            medicalOrder = order,
            admission = order.admission,
            status = request.status,
            notes = request.notes,
            lot = lot,
            quantity = if (request.status == AdministrationStatus.GIVEN) request.quantity else 0,
        )

        val saved = administrationRepository.save(administration)
        val adminUser = saved.createdBy?.let { userRepository.findById(it).orElse(null) }
        return MedicationAdministrationResponse.from(saved, adminUser, billable)
    }
}
