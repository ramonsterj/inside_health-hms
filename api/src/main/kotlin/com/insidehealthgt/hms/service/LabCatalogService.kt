package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateLabPanelRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.CreateLabTestRequest
import com.insidehealthgt.hms.dto.request.UpdateLabPanelRequest
import com.insidehealthgt.hms.dto.request.UpdateLabProviderRequest
import com.insidehealthgt.hms.dto.request.UpdateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.UpdateLabTestRequest
import com.insidehealthgt.hms.dto.response.LabPanelResponse
import com.insidehealthgt.hms.dto.response.LabProviderResponse
import com.insidehealthgt.hms.dto.response.LabProviderTestResponse
import com.insidehealthgt.hms.dto.response.LabTestResponse
import com.insidehealthgt.hms.dto.response.PanelResolutionResponse
import com.insidehealthgt.hms.dto.response.ResolvedPanelTest
import com.insidehealthgt.hms.dto.response.UnmatchedPanelTest
import com.insidehealthgt.hms.entity.LabPanel
import com.insidehealthgt.hms.entity.LabPanelItem
import com.insidehealthgt.hms.entity.LabProvider
import com.insidehealthgt.hms.entity.LabProviderTest
import com.insidehealthgt.hms.entity.LabTest
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.LabPanelRepository
import com.insidehealthgt.hms.repository.LabProviderRepository
import com.insidehealthgt.hms.repository.LabProviderTestRepository
import com.insidehealthgt.hms.repository.LabTestRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * CRUD for the lab catalog (providers, canonical tests, per-provider priced tests, panels)
 * plus panel resolution against a provider.
 *
 * All deletes are soft deletes. Providers and canonical tests cannot be deleted while
 * referenced by any (even soft-deleted) provider-test / panel-item row, mirroring
 * [InventoryCategoryService]. **Provider-test soft-delete is allowed even when referenced by
 * order lines**, because every [com.insidehealthgt.hms.entity.MedicalOrderLabTest] fully
 * snapshots the provider-test's name/cost/price at order time (AC12) — deleting the catalog
 * row can never change a recorded order or its already-created charge.
 */
@Service
@Suppress("TooManyFunctions")
class LabCatalogService(
    private val providerRepository: LabProviderRepository,
    private val testRepository: LabTestRepository,
    private val providerTestRepository: LabProviderTestRepository,
    private val panelRepository: LabPanelRepository,
    private val userRepository: UserRepository,
) {

    // ===================== Providers =====================

    @Transactional(readOnly = true)
    fun listProviders(activeOnly: Boolean): List<LabProviderResponse> {
        val providers = if (activeOnly) {
            providerRepository.findAllByActiveTrueOrderByNameAsc()
        } else {
            providerRepository.findAllByOrderByNameAsc()
        }
        return providers.map { LabProviderResponse.fromSimple(it) }
    }

    @Transactional
    fun createProvider(request: CreateLabProviderRequest): LabProviderResponse {
        if (providerRepository.existsByNameIgnoreCase(request.name)) {
            throw BadRequestException("A lab provider with name '${request.name}' already exists")
        }
        val provider = providerRepository.save(
            LabProvider(name = request.name, code = request.code?.takeIf { it.isNotBlank() }, active = request.active),
        )
        return buildProviderResponse(provider)
    }

    @Transactional
    fun updateProvider(id: Long, request: UpdateLabProviderRequest): LabProviderResponse {
        val provider = findProvider(id)
        if (providerRepository.existsByNameIgnoreCaseAndIdNot(request.name, id)) {
            throw BadRequestException("A lab provider with name '${request.name}' already exists")
        }
        provider.name = request.name
        provider.code = request.code?.takeIf { it.isNotBlank() }
        provider.active = request.active
        return buildProviderResponse(providerRepository.save(provider))
    }

    @Transactional
    fun deleteProvider(id: Long) {
        val provider = findProvider(id)
        if (providerRepository.existsProviderTestsByProviderIdIncludingDeleted(id)) {
            throw BadRequestException("Cannot delete a lab provider that has provider tests")
        }
        provider.deletedAt = LocalDateTime.now()
        providerRepository.save(provider)
    }

    // ===================== Canonical tests =====================

    @Transactional(readOnly = true)
    fun listTests(activeOnly: Boolean): List<LabTestResponse> {
        val tests = if (activeOnly) {
            testRepository.findAllByActiveTrueOrderByNameAsc()
        } else {
            testRepository.findAllByOrderByNameAsc()
        }
        return tests.map { LabTestResponse.fromSimple(it) }
    }

    @Transactional
    fun createTest(request: CreateLabTestRequest): LabTestResponse {
        if (testRepository.existsByNameIgnoreCase(request.name)) {
            throw BadRequestException("A lab test with name '${request.name}' already exists")
        }
        val test = testRepository.save(LabTest(name = request.name, active = request.active))
        return buildTestResponse(test)
    }

    @Transactional
    fun updateTest(id: Long, request: UpdateLabTestRequest): LabTestResponse {
        val test = findTest(id)
        if (testRepository.existsByNameIgnoreCaseAndIdNot(request.name, id)) {
            throw BadRequestException("A lab test with name '${request.name}' already exists")
        }
        test.name = request.name
        test.active = request.active
        return buildTestResponse(testRepository.save(test))
    }

    @Transactional
    fun deleteTest(id: Long) {
        val test = findTest(id)
        if (testRepository.existsReferencesByLabTestIdIncludingDeleted(id)) {
            throw BadRequestException("Cannot delete a lab test referenced by provider tests or panels")
        }
        test.deletedAt = LocalDateTime.now()
        testRepository.save(test)
    }

    // ===================== Provider tests =====================

    @Transactional(readOnly = true)
    fun listProviderTests(providerId: Long, activeOnly: Boolean): List<LabProviderTestResponse> {
        findProvider(providerId)
        val rows = if (activeOnly) {
            providerTestRepository.findByProviderIdAndActiveTrueOrderByDisplayNameAsc(providerId)
        } else {
            providerTestRepository.findByProviderIdOrderByDisplayNameAsc(providerId)
        }
        return rows.map { LabProviderTestResponse.from(it) }
    }

    @Transactional
    fun createProviderTest(providerId: Long, request: CreateLabProviderTestRequest): LabProviderTestResponse {
        val provider = findProvider(providerId)
        val test = findTest(request.labTestId)
        if (providerTestRepository.existsByProviderIdAndLabTestIdAndDeletedAtIsNull(providerId, request.labTestId)) {
            throw BadRequestException("This provider already offers the selected canonical test")
        }
        validatePricing(request.cost, request.salesPrice)

        val providerTest = providerTestRepository.save(
            LabProviderTest(
                provider = provider,
                labTest = test,
                displayName = request.displayName?.takeIf { it.isNotBlank() } ?: test.name,
                cost = request.cost,
                salesPrice = request.salesPrice,
                active = request.active,
            ),
        )
        return LabProviderTestResponse.from(providerTest)
    }

    @Transactional
    fun updateProviderTest(id: Long, request: UpdateLabProviderTestRequest): LabProviderTestResponse {
        val providerTest = providerTestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Lab provider test not found with id: $id") }
        validatePricing(request.cost, request.salesPrice)

        providerTest.displayName = request.displayName?.takeIf { it.isNotBlank() } ?: providerTest.labTest.name
        providerTest.cost = request.cost
        providerTest.salesPrice = request.salesPrice
        providerTest.active = request.active
        return LabProviderTestResponse.from(providerTestRepository.save(providerTest))
    }

    @Transactional
    fun deleteProviderTest(id: Long) {
        // Allowed even when referenced by order lines — lines are fully snapshotted (AC12).
        val providerTest = providerTestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Lab provider test not found with id: $id") }
        providerTest.deletedAt = LocalDateTime.now()
        providerTestRepository.save(providerTest)
    }

    // ===================== Panels =====================

    @Transactional(readOnly = true)
    fun listPanels(): List<LabPanelResponse> = panelRepository.findAllWithItems().map { LabPanelResponse.from(it) }

    @Transactional
    fun createPanel(request: CreateLabPanelRequest): LabPanelResponse {
        if (panelRepository.existsByNameIgnoreCase(request.name)) {
            throw BadRequestException("A lab panel with name '${request.name}' already exists")
        }
        val panel = LabPanel(name = request.name, active = request.active)
        replacePanelItems(panel, request.labTestIds)
        return LabPanelResponse.from(panelRepository.save(panel))
    }

    @Transactional
    fun updatePanel(id: Long, request: UpdateLabPanelRequest): LabPanelResponse {
        val panel = panelRepository.findByIdWithItems(id)
            ?: throw ResourceNotFoundException("Lab panel not found with id: $id")
        if (panelRepository.existsByNameIgnoreCaseAndIdNot(request.name, id)) {
            throw BadRequestException("A lab panel with name '${request.name}' already exists")
        }
        panel.name = request.name
        panel.active = request.active
        softDeletePanelItems(panel)
        panelRepository.flush()
        replacePanelItems(panel, request.labTestIds)
        return LabPanelResponse.from(panelRepository.save(panel))
    }

    private fun softDeletePanelItems(panel: LabPanel) {
        val now = LocalDateTime.now()
        panel.items
            .filter { it.deletedAt == null }
            .forEach { it.deletedAt = now }
    }

    @Transactional
    fun deletePanel(id: Long) {
        val panel = panelRepository.findByIdWithItems(id)
            ?: throw ResourceNotFoundException("Lab panel not found with id: $id")
        val now = LocalDateTime.now()
        panel.items.forEach { it.deletedAt = now }
        panel.deletedAt = now
        panelRepository.save(panel)
    }

    /**
     * Resolve a panel against a provider.
     *
     * Loads the (active) panel and the provider — the provider must be active, else
     * [BadRequestException]. `matched` is the provider's active offerings whose canonical
     * test is in the panel; `unmatchedTests` is every panel canonical test the provider does
     * **not** offer (so the client can warn the doctor and order them elsewhere). This method
     * is read-only and never mutates the panel, provider, or any order.
     */
    @Transactional(readOnly = true)
    fun resolvePanel(panelId: Long, providerId: Long): PanelResolutionResponse {
        val panel = panelRepository.findByIdWithItems(panelId)
            ?: throw ResourceNotFoundException("Lab panel not found with id: $panelId")
        val provider = findProvider(providerId)
        if (!provider.active) {
            throw BadRequestException("Lab provider '${provider.name}' is inactive")
        }

        val panelTests = panel.items.map { it.labTest }
        val offeringByTestId = providerTestRepository
            .findByProviderIdAndActiveTrueOrderByDisplayNameAsc(providerId)
            .associateBy { it.labTest.id }

        val matched = mutableListOf<ResolvedPanelTest>()
        val unmatched = mutableListOf<UnmatchedPanelTest>()
        for (test in panelTests) {
            val offering = offeringByTestId[test.id]
            if (offering != null) {
                matched.add(
                    ResolvedPanelTest(
                        labProviderTestId = offering.id!!,
                        labTestId = test.id!!,
                        displayName = offering.displayName,
                        salesPrice = offering.salesPrice,
                    ),
                )
            } else {
                unmatched.add(UnmatchedPanelTest(labTestId = test.id!!, name = test.name))
            }
        }

        return PanelResolutionResponse(
            panelId = panelId,
            providerId = providerId,
            matched = matched,
            unmatchedTests = unmatched,
        )
    }

    // ===================== Helpers =====================

    private fun replacePanelItems(panel: LabPanel, labTestIds: List<Long>) {
        val distinctIds = labTestIds.distinct()
        val tests = testRepository.findAllByIdInAndActiveTrue(distinctIds)
        if (tests.size != distinctIds.size) {
            throw BadRequestException("One or more canonical tests are invalid or inactive")
        }
        tests.forEach { panel.items.add(LabPanelItem(panel = panel, labTest = it)) }
    }

    private fun validatePricing(cost: BigDecimal, salesPrice: BigDecimal) {
        if (salesPrice <= BigDecimal.ZERO) {
            throw BadRequestException("Sales price must be greater than zero")
        }
        if (cost < BigDecimal.ZERO) {
            throw BadRequestException("Cost must not be negative")
        }
    }

    private fun findProvider(id: Long): LabProvider = providerRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Lab provider not found with id: $id") }

    private fun findTest(id: Long): LabTest = testRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Lab test not found with id: $id") }

    private fun buildProviderResponse(provider: LabProvider): LabProviderResponse {
        val createdByUser = provider.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = provider.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return LabProviderResponse.from(provider, createdByUser, updatedByUser)
    }

    private fun buildTestResponse(test: LabTest): LabTestResponse {
        val createdByUser = test.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = test.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return LabTestResponse.from(test, createdByUser, updatedByUser)
    }
}
