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
import com.insidehealthgt.hms.entity.MedicalOrderLabTest
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.event.LabLineSnapshot
import com.insidehealthgt.hms.event.LabOrderAuthorizedEvent
import com.insidehealthgt.hms.event.MedicalOrderAuthorizedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.LabProviderRepository
import com.insidehealthgt.hms.repository.LabProviderTestRepository
import com.insidehealthgt.hms.repository.MedicalOrderDocumentRepository
import com.insidehealthgt.hms.repository.MedicalOrderLabTestRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class MedicalOrderService(
    private val medicalOrderRepository: MedicalOrderRepository,
    private val medicalOrderDocumentRepository: MedicalOrderDocumentRepository,
    private val admissionRepository: AdmissionRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val labProviderRepository: LabProviderRepository,
    private val labProviderTestRepository: LabProviderTestRepository,
    private val medicalOrderLabTestRepository: MedicalOrderLabTestRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val currentUserProvider: CurrentUserProvider,
    private val messageService: MessageService,
) {

    private val log = LoggerFactory.getLogger(MedicalOrderService::class.java)

    @Transactional(readOnly = true)
    fun listMedicalOrders(admissionId: Long): GroupedMedicalOrdersResponse {
        verifyAdmissionExists(admissionId)

        // Psychologists may only see PRUEBAS_PSICOMETRICAS orders, mirroring the per-order
        // scope guard. Without this filter the admission-scoped listing would leak meds,
        // labs, and referrals to anyone with the medical-order:read permission (granted to
        // PSICOLOGO by V116).
        val orders = medicalOrderRepository.findByAdmissionIdWithRelations(admissionId)
            .filterNot { isPsychologistOutOfScope(it.category) }
        val users = loadAuditUsers(orders)
        val orderIds = orders.mapNotNull { it.id }
        val documentCounts = loadDocumentCounts(orderIds)
        val labLines = loadLabLines(orderIds)

        val groupedOrders = orders
            .map { buildResponse(it, users, documentCounts[it.id] ?: 0, labLines[it.id].orEmpty()) }
            .groupBy { it.category }

        return GroupedMedicalOrdersResponse(orders = groupedOrders)
    }

    @Transactional(readOnly = true)
    fun getMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (isPsychologistOutOfScope(order.category)) {
            throw ResourceNotFoundException(
                "Medical order not found with id: $orderId for admission: $admissionId",
            )
        }

        return buildResponse(order)
    }

    @Transactional(readOnly = true)
    fun listOrdersByStatus(
        statuses: List<MedicalOrderStatus>?,
        categories: List<MedicalOrderCategory>?,
        pageable: Pageable,
    ): Page<MedicalOrderListItemResponse> {
        // For psychologists the requested category filter is *intersected* with their
        // PRUEBAS_PSICOMETRICAS scope rather than replaced — replacing it returned
        // psychometric orders for requests like ?category=LABORATORIOS, breaking the
        // filter contract. If the intersection is empty the response is empty.
        val effectiveCategories = if (isPsychologistOnly()) {
            val scope = MedicalOrderCategory.PRUEBAS_PSICOMETRICAS
            when {
                categories.isNullOrEmpty() -> listOf(scope)
                scope in categories -> listOf(scope)
                else -> return Page.empty(pageable)
            }
        } else {
            categories
        }
        val page = medicalOrderRepository.findByFilters(statuses, effectiveCategories, pageable)

        val users = loadUsers(page.content.mapNotNull { it.createdBy })
        val orderIds = page.content.mapNotNull { it.id }
        val documentCounts = loadDocumentCounts(orderIds)
        val labAggregates = if (orderIds.isEmpty()) {
            emptyMap()
        } else {
            medicalOrderLabTestRepository.aggregateByOrderIds(orderIds).associateBy { it.orderId }
        }

        return page.map { order ->
            val labAgg = labAggregates[order.id]
            MedicalOrderListItemResponse.from(
                order = order,
                createdByUser = order.createdBy?.let { users[it] },
                documentCount = documentCounts[order.id] ?: 0,
                labProviderName = order.labProvider?.name,
                labTotal = labAgg?.total,
                labTestCount = labAgg?.lineCount?.toInt(),
            )
        }
    }

    @Transactional
    fun createMedicalOrder(admissionId: Long, request: CreateMedicalOrderRequest): MedicalOrderResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionDischargedRecords())
        }

        // Every LABORATORIOS order is provider-catalog backed (one provider + >= 1 line);
        // there is no legacy single-inventory-item lab path. buildLabLines enforces the
        // provider/lines invariants below. For lab orders the inventory item is unused.
        val isLabOrder = request.category == MedicalOrderCategory.LABORATORIOS
        val inventoryItem = if (isLabOrder) {
            null
        } else {
            request.inventoryItemId?.let { itemId ->
                inventoryItemRepository.findById(itemId)
                    .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $itemId") }
            }
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

        if (isLabOrder) {
            buildLabLines(request, order)
        }

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    /**
     * Resolve and snapshot lab provider-test lines onto a `LABORATORIOS` order. Validates:
     * a provider is given (400), at least one line (400, AC8), every referenced provider-test
     * exists and is active (400, AC11), and every line belongs to the order's provider
     * (400, AC7 — one order = one provider). Each line snapshots display name / cost / sales
     * price so later catalog edits never change the recorded order or its bill (AC12).
     */
    @Suppress("ThrowsCount")
    private fun buildLabLines(request: CreateMedicalOrderRequest, order: MedicalOrder) {
        val providerId = request.labProviderId
            ?: throw BadRequestException("A lab provider is required for laboratory orders")
        val testIds = request.labProviderTestIds?.distinct().orEmpty()
        if (testIds.isEmpty()) {
            throw BadRequestException("At least one lab test is required for laboratory orders")
        }

        val provider = labProviderRepository.findById(providerId)
            .orElseThrow { ResourceNotFoundException("Lab provider not found with id: $providerId") }

        val providerTests = labProviderTestRepository.findAllByIdInAndActiveTrue(testIds)
        if (providerTests.size != testIds.size) {
            throw BadRequestException("One or more lab tests are invalid or inactive")
        }
        if (providerTests.any { it.provider.id != providerId }) {
            throw BadRequestException("All lab tests must belong to the selected provider")
        }

        order.labProvider = provider
        providerTests.forEach { pt ->
            order.labTests.add(
                MedicalOrderLabTest(
                    medicalOrder = order,
                    labProviderTestId = pt.id!!,
                    labTestId = pt.labTest.id!!,
                    displayName = pt.displayName,
                    cost = pt.cost,
                    salesPrice = pt.salesPrice,
                ),
            )
        }
    }

    @Transactional
    @Suppress("ThrowsCount")
    fun updateMedicalOrder(
        admissionId: Long,
        orderId: Long,
        request: UpdateMedicalOrderRequest,
    ): MedicalOrderResponse {
        validateAdmissionActive(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status.isTerminal()) {
            throw BadRequestException("Cannot update a medical order in a terminal state (${order.status})")
        }

        val willBeLab = request.category == MedicalOrderCategory.LABORATORIOS
        val wasLab = order.labProvider != null || order.labTests.isNotEmpty()

        // AC14: provider/line snapshots are immutable once the order has produced its bill.
        // Lab lines may only be (re)built or cleared while SOLICITADO (terminal states are
        // already blocked above; this also rejects AUTORIZADO / EN_PROCESO /
        // RESULTADOS_RECIBIDOS). Guards both becoming a lab order and an existing lab order
        // changing to another category (which would drop its already-recorded lines).
        if ((willBeLab || wasLab) && order.status != MedicalOrderStatus.SOLICITADO) {
            throw BadRequestException(
                "Lab provider and tests can only be changed while the order is in SOLICITADO; " +
                    "current state is ${order.status}",
            )
        }

        val inventoryItem = if (willBeLab) {
            null
        } else {
            request.inventoryItemId?.let { itemId ->
                inventoryItemRepository.findById(itemId)
                    .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $itemId") }
            }
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

        if (willBeLab) {
            softDeleteLabLines(order)
            order.labProvider = null
            buildLabLines(request, order)
        } else if (wasLab) {
            // Changing a lab order to another category: drop the now-stale provider/lines so
            // the order is never left in a mixed shape.
            softDeleteLabLines(order)
            order.labProvider = null
        }

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    private fun softDeleteLabLines(order: MedicalOrder) {
        val now = LocalDateTime.now()
        order.labTests
            .filter { it.deletedAt == null }
            .forEach { it.deletedAt = now }
    }

    @Transactional
    fun discontinueMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        validateAdmissionActive(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status !in MedicalOrderStatus.DISCONTINUABLE_STATES) {
            throw BadRequestException(
                "Cannot discontinue a medical order in state ${order.status}. " +
                    "Discontinue is only allowed from ACTIVA, SOLICITADO, or AUTORIZADO " +
                    "(once a sample is taken / referral made, it cannot be cancelled).",
            )
        }

        val currentUserId = currentUserProvider.currentUserIdOrThrow()

        order.status = MedicalOrderStatus.DESCONTINUADO
        order.discontinuedAt = LocalDateTime.now()
        order.discontinuedBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun authorize(admissionId: Long, orderId: Long): MedicalOrderResponse {
        val order = loadAuthorizableOrder(admissionId, orderId)

        val currentUserId = currentUserProvider.currentUserIdOrThrow()
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

        val currentUserId = currentUserProvider.currentUserIdOrThrow()
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
        validateAdmissionActive(admissionId)

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

    @Suppress("ReturnCount")
    private fun publishBillingEventIfNeeded(order: MedicalOrder) {
        // Only results-bearing categories (labs, referrals, psychometric tests) are billed
        // once on authorization. MEDICAMENTOS is billed per-administration via
        // InventoryDispensedEvent, so it must be excluded here to avoid double-charging.
        if (!order.category.supportsResults()) return

        // Lab orders bill the sum of their line sales-price snapshots as a single LAB charge.
        // REFERENCIAS / PRUEBAS fall through to the inventoryItem path below. The total guard
        // ensures a zero/garbage charge is never published.
        if (order.category == MedicalOrderCategory.LABORATORIOS && order.labTests.isNotEmpty()) {
            val total = order.labTotal()
            if (total <= BigDecimal.ZERO) {
                log.warn("Authorized lab order {} has a non-positive total, no auto-charge created", order.id)
                return
            }
            eventPublisher.publishEvent(
                LabOrderAuthorizedEvent(
                    admissionId = order.admission.id!!,
                    medicalOrderId = order.id!!,
                    providerName = order.labProvider?.name ?: "",
                    lines = order.labTests.map { LabLineSnapshot(it.displayName, it.salesPrice, it.cost) },
                    lineTotal = total,
                ),
            )
            return
        }

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

        val currentUserId = currentUserProvider.currentUserIdOrThrow()

        order.status = MedicalOrderStatus.NO_AUTORIZADO
        order.rejectedAt = LocalDateTime.now()
        order.rejectedBy = currentUserId
        order.rejectionReason = request.reason

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun markInProgress(admissionId: Long, orderId: Long): MedicalOrderResponse {
        // Auxiliary nurses cannot mark orders in progress (belt-and-suspenders over @PreAuthorize).
        currentUserProvider.requireNotAuxiliaryNurseOnly()

        validateAdmissionActive(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (isPsychologistOutOfScope(order.category)) {
            throw BadRequestException(messageService.errorMedicalOrderPsychologistCategoryScope())
        }

        requireResultsBearingAndAuthorized(order)

        val currentUserId = currentUserProvider.currentUserIdOrThrow()
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

    private fun isPsychologistOnly(): Boolean {
        val details = currentUserProvider.currentUserDetails()
        return details != null &&
            details.hasRole("PSICOLOGO") &&
            MEDICAL_ORDER_BROAD_ROLES.none { details.hasRole(it) }
    }

    /**
     * Psychologists may only act on (and see) PRUEBAS_PSICOMETRICAS orders.
     * Returns true when the current user is a psychologist — without any
     * broader role — and the order's category falls outside that scope.
     */
    fun isPsychologistOutOfScope(category: MedicalOrderCategory): Boolean =
        isPsychologistOnly() && category != MedicalOrderCategory.PRUEBAS_PSICOMETRICAS

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    /**
     * Discharge protection: a discharged admission's record is immutable. Every order
     * mutation (create / update / state transition) routes through here. Reads stay on
     * [verifyAdmissionExists] so history remains viewable post-discharge.
     */
    private fun validateAdmissionActive(admissionId: Long) {
        val admission = admissionRepository.findById(admissionId).orElseThrow {
            ResourceNotFoundException("Admission not found with id: $admissionId")
        }
        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionDischargedRecords())
        }
    }

    private fun buildResponse(order: MedicalOrder): MedicalOrderResponse {
        val labLines = loadLabLines(listOfNotNull(order.id))[order.id].orEmpty()
        return buildResponse(order, loadAuditUsers(listOf(order)), documentCount = 0, labLines = labLines)
    }

    private fun buildResponse(
        order: MedicalOrder,
        users: Map<Long, User>,
        documentCount: Int,
        labLines: List<MedicalOrderLabTest> = emptyList(),
    ): MedicalOrderResponse = MedicalOrderResponse.from(
        medicalOrder = order,
        createdByUser = order.createdBy?.let { users[it] },
        updatedByUser = order.updatedBy?.let { users[it] },
        discontinuedByUser = order.discontinuedBy?.let { users[it] },
        authorizedByUser = order.authorizedBy?.let { users[it] },
        rejectedByUser = order.rejectedBy?.let { users[it] },
        inProgressByUser = order.inProgressBy?.let { users[it] },
        resultsReceivedByUser = order.resultsReceivedBy?.let { users[it] },
        emergencyByUser = order.emergencyBy?.let { users[it] },
        documentCount = documentCount,
        labTests = labLines,
    )

    /** Batch-load lab line items grouped by order id (avoids N+1 and MultipleBagFetchException). */
    private fun loadLabLines(orderIds: List<Long>): Map<Long, List<MedicalOrderLabTest>> {
        if (orderIds.isEmpty()) return emptyMap()
        return medicalOrderLabTestRepository.findByMedicalOrderIdIn(orderIds)
            .groupBy { it.medicalOrder.id!! }
    }

    private fun loadAuditUsers(orders: List<MedicalOrder>): Map<Long, User> = loadUsers(
        orders.flatMap {
            listOfNotNull(
                it.createdBy,
                it.updatedBy,
                it.discontinuedBy,
                it.authorizedBy,
                it.rejectedBy,
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

    companion object {
        private val MEDICAL_ORDER_BROAD_ROLES = setOf(
            "ADMINISTRADOR",
            "MEDICO",
            "MEDICO_RESIDENTE",
            "ENFERMERO",
            "JEFE_ENFERMERIA",
            "PERSONAL_ADMINISTRATIVO",
        )
    }
}
