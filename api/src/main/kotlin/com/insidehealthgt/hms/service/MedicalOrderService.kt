package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.EmergencyAuthorizeMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.RejectMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.dto.response.GroupedMedicalOrdersResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderListItemResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderResponse
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.event.MedicalOrderAuthorizedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.MedicalOrderDocumentRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class MedicalOrderService(
    private val medicalOrderRepository: MedicalOrderRepository,
    private val medicalOrderDocumentRepository: MedicalOrderDocumentRepository,
    private val admissionRepository: AdmissionRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(MedicalOrderService::class.java)

    @Transactional(readOnly = true)
    fun listMedicalOrders(admissionId: Long): GroupedMedicalOrdersResponse {
        verifyAdmissionExists(admissionId)

        val orders = medicalOrderRepository.findByAdmissionIdWithRelations(admissionId)
        val users = loadAuditUsers(orders)
        val documentCounts = loadDocumentCounts(orders.mapNotNull { it.id })

        val groupedOrders = orders
            .map { buildResponse(it, users, documentCounts[it.id] ?: 0) }
            .groupBy { it.category }

        return GroupedMedicalOrdersResponse(orders = groupedOrders)
    }

    @Transactional(readOnly = true)
    fun getMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        return buildResponse(order)
    }

    @Transactional(readOnly = true)
    fun listOrdersByStatus(
        statuses: List<MedicalOrderStatus>?,
        categories: List<MedicalOrderCategory>?,
        pageable: Pageable,
    ): Page<MedicalOrderListItemResponse> {
        val page = medicalOrderRepository.findByFilters(statuses, categories, pageable)

        val users = loadUsers(page.content.mapNotNull { it.createdBy })
        val documentCounts = loadDocumentCounts(page.content.mapNotNull { it.id })

        return page.map { order ->
            MedicalOrderListItemResponse.from(
                order = order,
                createdByUser = order.createdBy?.let { users[it] },
                documentCount = documentCounts[order.id] ?: 0,
            )
        }
    }

    @Transactional
    fun createMedicalOrder(admissionId: Long, request: CreateMedicalOrderRequest): MedicalOrderResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val inventoryItem = request.inventoryItemId?.let { itemId ->
            inventoryItemRepository.findById(itemId)
                .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $itemId") }
        }

        // Initial status is category-driven: directive → ACTIVA, auth-required → SOLICITADO.
        val order = MedicalOrder(
            admission = admission,
            category = request.category,
            startDate = request.startDate,
            endDate = request.endDate,
            medication = request.medication,
            dosage = request.dosage,
            route = request.route,
            frequency = request.frequency,
            schedule = request.schedule,
            observations = request.observations,
            status = request.category.initialStatus(),
            inventoryItem = inventoryItem,
        )

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun updateMedicalOrder(
        admissionId: Long,
        orderId: Long,
        request: UpdateMedicalOrderRequest,
    ): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status.isTerminal()) {
            throw BadRequestException("Cannot update a medical order in a terminal state (${order.status})")
        }

        val inventoryItem = request.inventoryItemId?.let { itemId ->
            inventoryItemRepository.findById(itemId)
                .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $itemId") }
        }

        order.category = request.category
        order.startDate = request.startDate
        order.endDate = request.endDate
        order.medication = request.medication
        order.dosage = request.dosage
        order.route = request.route
        order.frequency = request.frequency
        order.schedule = request.schedule
        order.observations = request.observations
        order.inventoryItem = inventoryItem

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun discontinueMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status !in MedicalOrderStatus.DISCONTINUABLE_STATES) {
            throw BadRequestException(
                "Cannot discontinue a medical order in state ${order.status}. " +
                    "Discontinue is only allowed from ACTIVA, SOLICITADO, or AUTORIZADO " +
                    "(once a sample is taken / referral made, it cannot be cancelled).",
            )
        }

        val currentUserId = getCurrentUserId()

        order.status = MedicalOrderStatus.DESCONTINUADO
        order.discontinuedAt = LocalDateTime.now()
        order.discontinuedBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun authorize(admissionId: Long, orderId: Long): MedicalOrderResponse {
        val order = loadAuthorizableOrder(admissionId, orderId)

        val currentUserId = getCurrentUserId()
        order.status = MedicalOrderStatus.AUTORIZADO
        order.authorizedAt = LocalDateTime.now()
        order.authorizedBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        publishBillingEventIfNeeded(saved)

        return buildResponse(saved)
    }

    @Transactional
    fun emergencyAuthorize(
        admissionId: Long,
        orderId: Long,
        request: EmergencyAuthorizeMedicalOrderRequest,
    ): MedicalOrderResponse {
        val reason = request.reason
            ?: throw BadRequestException("Emergency authorization reason is required")
        if (reason.requiresNote() && request.reasonNote.isNullOrBlank()) {
            throw BadRequestException("reasonNote is required when reason is OTHER")
        }

        val order = loadAuthorizableOrder(admissionId, orderId)

        val currentUserId = getCurrentUserId()
        val now = LocalDateTime.now()

        order.status = MedicalOrderStatus.AUTORIZADO
        order.authorizedAt = now
        order.authorizedBy = currentUserId
        order.emergencyAuthorized = true
        order.emergencyReason = reason
        order.emergencyReasonNote = request.reasonNote?.takeIf { it.isNotBlank() }
        order.emergencyAt = now
        order.emergencyBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        publishBillingEventIfNeeded(saved)

        return buildResponse(saved)
    }

    private fun loadAuthorizableOrder(admissionId: Long, orderId: Long): MedicalOrder {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException(
                "Medical order not found with id: $orderId for admission: $admissionId",
            )

        requireAwaitingAuthorization(order)
        return order
    }

    private fun requireAwaitingAuthorization(order: MedicalOrder) {
        if (!order.category.requiresAuthorization()) {
            throw BadRequestException(
                "Category ${order.category} does not require authorization (it is a directive order)",
            )
        }
        if (order.status != MedicalOrderStatus.SOLICITADO) {
            throw BadRequestException(
                "Only orders in SOLICITADO can be authorized or rejected; current state is ${order.status}",
            )
        }
    }

    private fun publishBillingEventIfNeeded(order: MedicalOrder) {
        // Only results-bearing categories (labs, referrals, psychometric tests) are billed
        // once on authorization. MEDICAMENTOS is billed per-administration via
        // InventoryDispensedEvent, so it must be excluded here to avoid double-charging.
        if (!order.category.supportsResults()) return

        val inventoryItem = order.inventoryItem
        if (inventoryItem == null) {
            log.info(
                "Authorized medical order {} has no linked inventory item, no auto-charge created",
                order.id,
            )
            return
        }

        eventPublisher.publishEvent(
            MedicalOrderAuthorizedEvent(
                admissionId = order.admission.id!!,
                category = order.category,
                inventoryItemId = inventoryItem.id!!,
                itemName = inventoryItem.name,
                unitPrice = inventoryItem.price,
            ),
        )
    }

    @Transactional
    fun reject(admissionId: Long, orderId: Long, request: RejectMedicalOrderRequest): MedicalOrderResponse {
        val order = loadAuthorizableOrder(admissionId, orderId)

        val currentUserId = getCurrentUserId()

        order.status = MedicalOrderStatus.NO_AUTORIZADO
        order.authorizedAt = LocalDateTime.now()
        order.authorizedBy = currentUserId
        order.rejectionReason = request.reason

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun markInProgress(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        requireResultsBearingAndAuthorized(order)

        val currentUserId = getCurrentUserId()
        order.status = MedicalOrderStatus.EN_PROCESO
        order.inProgressAt = LocalDateTime.now()
        order.inProgressBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    private fun requireResultsBearingAndAuthorized(order: MedicalOrder) {
        if (!order.category.supportsResults()) {
            throw BadRequestException("Category ${order.category} does not support an in-progress phase")
        }
        if (order.status != MedicalOrderStatus.AUTORIZADO) {
            throw BadRequestException(
                "Only orders in AUTORIZADO can be marked in progress; current state is ${order.status}",
            )
        }
    }

    /**
     * Called from [MedicalOrderDocumentService.uploadDocument] when a result document is
     * attached to a results-bearing order in [MedicalOrderStatus.AUTORIZADO] or
     * [MedicalOrderStatus.EN_PROCESO]. Stamps audit fields and transitions the order to
     * [MedicalOrderStatus.RESULTADOS_RECIBIDOS] in the same transaction.
     *
     * No-op for any other state — this enforces the rule that RESULTADOS_RECIBIDOS is only
     * reachable by uploading a document.
     */
    @Transactional
    fun markResultsReceivedFromDocumentUpload(order: MedicalOrder, uploaderId: Long): MedicalOrder {
        if (order.status != MedicalOrderStatus.AUTORIZADO && order.status != MedicalOrderStatus.EN_PROCESO) {
            return order
        }

        order.status = MedicalOrderStatus.RESULTADOS_RECIBIDOS
        order.resultsReceivedAt = LocalDateTime.now()
        order.resultsReceivedBy = uploaderId

        return medicalOrderRepository.save(order)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun getCurrentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal
        return if (principal is CustomUserDetails) {
            principal.id
        } else {
            throw IllegalStateException("Unable to get current user ID")
        }
    }

    private fun buildResponse(order: MedicalOrder): MedicalOrderResponse =
        buildResponse(order, loadAuditUsers(listOf(order)), documentCount = 0)

    private fun buildResponse(order: MedicalOrder, users: Map<Long, User>, documentCount: Int): MedicalOrderResponse =
        MedicalOrderResponse.from(
            medicalOrder = order,
            createdByUser = order.createdBy?.let { users[it] },
            updatedByUser = order.updatedBy?.let { users[it] },
            discontinuedByUser = order.discontinuedBy?.let { users[it] },
            authorizedByUser = order.authorizedBy?.let { users[it] },
            inProgressByUser = order.inProgressBy?.let { users[it] },
            resultsReceivedByUser = order.resultsReceivedBy?.let { users[it] },
            emergencyByUser = order.emergencyBy?.let { users[it] },
            documentCount = documentCount,
        )

    private fun loadAuditUsers(orders: List<MedicalOrder>): Map<Long, User> = loadUsers(
        orders.flatMap {
            listOfNotNull(
                it.createdBy,
                it.updatedBy,
                it.discontinuedBy,
                it.authorizedBy,
                it.inProgressBy,
                it.resultsReceivedBy,
                it.emergencyBy,
            )
        },
    )

    private fun loadUsers(userIds: List<Long>): Map<Long, User> {
        val distinct = userIds.distinct()
        if (distinct.isEmpty()) return emptyMap()
        return userRepository.findAllById(distinct).associateBy { it.id!! }
    }

    private fun loadDocumentCounts(orderIds: List<Long>): Map<Long, Int> {
        if (orderIds.isEmpty()) return emptyMap()
        return medicalOrderDocumentRepository.countByMedicalOrderIds(orderIds)
            .associate { (it[0] as Long) to (it[1] as Long).toInt() }
    }
}
