package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateAdjustmentRequest
import com.insidehealthgt.hms.dto.request.CreateChargeRequest
import com.insidehealthgt.hms.dto.response.AdmissionBalanceResponse
import com.insidehealthgt.hms.dto.response.DailyChargeGroup
import com.insidehealthgt.hms.dto.response.DailyChargeItem
import com.insidehealthgt.hms.dto.response.PatientChargeResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.PatientCharge
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.event.AdmissionCreatedEvent
import com.insidehealthgt.hms.event.InventoryDispensedEvent
import com.insidehealthgt.hms.event.MedicalOrderCreatedEvent
import com.insidehealthgt.hms.event.PsychotherapyActivityCreatedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Suppress("TooManyFunctions")
class BillingService(
    private val chargeRepository: PatientChargeRepository,
    private val admissionRepository: AdmissionRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val userRepository: UserRepository,
    @Value("\${app.billing.daily-meal-rate:#{null}}") private val dailyMealRate: BigDecimal?,
    @Value("\${app.billing.electroshock-base-price:#{null}}") private val electroshockBasePrice: BigDecimal?,
    @Value("\${app.billing.ketamine-base-price:#{null}}") private val ketamineBasePrice: BigDecimal?,
) {

    private val log = LoggerFactory.getLogger(BillingService::class.java)

    @Transactional(readOnly = true)
    fun getCharges(admissionId: Long): List<PatientChargeResponse> {
        validateAdmissionExists(admissionId)
        val charges = chargeRepository.findByAdmissionIdOrderByChargeDateDesc(admissionId)

        val userIds = charges.mapNotNull { it.createdBy }.distinct()
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return charges.map { charge ->
            val createdByUser = charge.createdBy?.let { usersById[it] }
            PatientChargeResponse.from(charge, createdByUser)
        }
    }

    @Transactional(readOnly = true)
    fun getBalance(admissionId: Long): AdmissionBalanceResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val charges = chargeRepository.findByAdmissionIdOrderByChargeDateDesc(admissionId)

        val patient = admission.patient
        val patientName = "${patient.firstName} ${patient.lastName}"

        if (charges.isEmpty()) {
            return AdmissionBalanceResponse(
                admissionId = admissionId,
                patientName = patientName,
                admissionDate = admission.admissionDate.toLocalDate(),
                totalBalance = BigDecimal.ZERO,
                dailyBreakdown = emptyList(),
            )
        }

        val grouped = charges.groupBy { it.chargeDate }
            .toSortedMap()

        var cumulative = BigDecimal.ZERO
        val dailyBreakdown = grouped.map { (date, dayCharges) ->
            val dailyTotal = dayCharges.sumOf { it.totalAmount }
            cumulative = cumulative.add(dailyTotal)

            DailyChargeGroup(
                date = date,
                charges = dayCharges.map { charge ->
                    DailyChargeItem(
                        id = charge.id!!,
                        chargeType = charge.chargeType,
                        description = charge.description,
                        quantity = charge.quantity,
                        unitPrice = charge.unitPrice,
                        totalAmount = charge.totalAmount,
                    )
                },
                dailyTotal = dailyTotal,
                cumulativeTotal = cumulative,
            )
        }

        return AdmissionBalanceResponse(
            admissionId = admissionId,
            patientName = patientName,
            admissionDate = admission.admissionDate.toLocalDate(),
            totalBalance = cumulative,
            dailyBreakdown = dailyBreakdown,
        )
    }

    @Transactional
    fun createManualCharge(admissionId: Long, request: CreateChargeRequest): PatientChargeResponse {
        val admission = findActiveAdmission(admissionId)

        val allowedTypes = setOf(ChargeType.MEDICATION, ChargeType.PROCEDURE, ChargeType.LAB, ChargeType.SERVICE)
        if (request.chargeType !in allowedTypes) {
            throw BadRequestException("Charge type ${request.chargeType} is not allowed for manual charges")
        }

        val inventoryItem = request.inventoryItemId?.let { itemId ->
            inventoryItemRepository.findById(itemId)
                .orElseThrow { ResourceNotFoundException("Inventory item not found with id: $itemId") }
        }

        val totalAmount = request.unitPrice.multiply(BigDecimal(request.quantity))

        val charge = PatientCharge(
            admission = admission,
            chargeType = request.chargeType,
            description = request.description,
            quantity = request.quantity,
            unitPrice = request.unitPrice,
            totalAmount = totalAmount,
            inventoryItem = inventoryItem,
        )

        val savedCharge = chargeRepository.save(charge)
        val createdByUser = savedCharge.createdBy?.let { userRepository.findById(it).orElse(null) }
        return PatientChargeResponse.from(savedCharge, createdByUser)
    }

    @Transactional
    fun createAdjustment(admissionId: Long, request: CreateAdjustmentRequest): PatientChargeResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.ADJUSTMENT,
            description = request.description,
            quantity = 1,
            unitPrice = request.amount,
            totalAmount = request.amount,
            reason = request.reason,
        )

        val savedCharge = chargeRepository.save(charge)
        val createdByUser = savedCharge.createdBy?.let { userRepository.findById(it).orElse(null) }
        return PatientChargeResponse.from(savedCharge, createdByUser)
    }

    @Transactional
    fun createChargeFromInventoryDispensed(event: InventoryDispensedEvent) {
        val admission = admissionRepository.findByIdWithRelations(event.admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: ${event.admissionId}")

        val inventoryItem = inventoryItemRepository.findById(event.inventoryItemId)
            .orElseThrow { ResourceNotFoundException("Inventory item not found with id: ${event.inventoryItemId}") }

        val totalAmount = event.unitPrice.multiply(BigDecimal(event.quantity))

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.MEDICATION,
            description = event.itemName,
            quantity = event.quantity,
            unitPrice = event.unitPrice,
            totalAmount = totalAmount,
            inventoryItem = inventoryItem,
        )

        chargeRepository.save(charge)
        log.info(
            "Created MEDICATION charge for admission {} from inventory item {}",
            event.admissionId,
            event.inventoryItemId,
        )
    }

    @Transactional
    fun createChargeFromPsychotherapyActivity(event: PsychotherapyActivityCreatedEvent) {
        val admission = admissionRepository.findByIdWithRelations(event.admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: ${event.admissionId}")

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.SERVICE,
            description = "Psychotherapy - ${event.categoryName}",
            quantity = 1,
            unitPrice = event.price,
            totalAmount = event.price,
        )

        chargeRepository.save(charge)
        log.info(
            "Created SERVICE charge for admission {} from psychotherapy activity '{}'",
            event.admissionId,
            event.categoryName,
        )
    }

    @Transactional
    fun createChargeFromMedicalOrder(event: MedicalOrderCreatedEvent) {
        val admission = admissionRepository.findByIdWithRelations(event.admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: ${event.admissionId}")

        val inventoryItem = inventoryItemRepository.findById(event.inventoryItemId)
            .orElseThrow { ResourceNotFoundException("Inventory item not found with id: ${event.inventoryItemId}") }

        val chargeType = mapCategoryToChargeType(event.category)

        val charge = PatientCharge(
            admission = admission,
            chargeType = chargeType,
            description = event.itemName,
            quantity = 1,
            unitPrice = event.unitPrice,
            totalAmount = event.unitPrice,
            inventoryItem = inventoryItem,
        )

        chargeRepository.save(charge)
        log.info(
            "Created {} charge for admission {} from medical order category {}",
            chargeType,
            event.admissionId,
            event.category,
        )
    }

    @Transactional
    fun createChargeFromAdmission(event: AdmissionCreatedEvent) {
        val basePrice = when (event.admissionType) {
            AdmissionType.ELECTROSHOCK_THERAPY -> electroshockBasePrice
            AdmissionType.KETAMINE_INFUSION -> ketamineBasePrice
            else -> return
        }

        if (basePrice == null || basePrice <= BigDecimal.ZERO) {
            log.warn(
                "No base price configured for admission type {}, skipping charge",
                event.admissionType,
            )
            return
        }

        val admission = admissionRepository.findById(event.admissionId)
            .orElseThrow { ResourceNotFoundException("Admission not found with id: ${event.admissionId}") }

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.PROCEDURE,
            description = "${event.admissionType.name.replace('_', ' ')} Procedure",
            quantity = 1,
            unitPrice = basePrice,
            totalAmount = basePrice,
        )

        chargeRepository.save(charge)
        log.info(
            "Created PROCEDURE charge for admission {} type {}",
            event.admissionId,
            event.admissionType,
        )
    }

    @Transactional
    fun createRoomCharge(admissionId: Long, room: Room, chargeDate: LocalDate) {
        val alreadyExists = chargeRepository.existsByAdmissionIdAndChargeTypeAndChargeDateAndRoomId(
            admissionId,
            ChargeType.ROOM,
            chargeDate,
            room.id!!,
        )

        if (alreadyExists) {
            log.debug("Room charge already exists for admission {} date {} room {}", admissionId, chargeDate, room.id)
            return
        }

        val admission = admissionRepository.findById(admissionId)
            .orElseThrow { ResourceNotFoundException("Admission not found with id: $admissionId") }

        val price = room.price!!

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.ROOM,
            description = "Room ${room.number} - Daily Rate",
            quantity = 1,
            unitPrice = price,
            totalAmount = price,
            chargeDate = chargeDate,
            room = room,
        )

        chargeRepository.save(charge)
        log.info("Created ROOM charge for admission {} date {} room {}", admissionId, chargeDate, room.number)
    }

    @Transactional
    fun createDietCharge(admissionId: Long, chargeDate: LocalDate) {
        val mealRate = dailyMealRate
        if (mealRate == null || mealRate <= BigDecimal.ZERO) {
            return
        }

        val alreadyExists = chargeRepository.existsByAdmissionIdAndChargeTypeAndChargeDate(
            admissionId,
            ChargeType.DIET,
            chargeDate,
        )

        if (alreadyExists) {
            log.debug("Diet charge already exists for admission {} date {}", admissionId, chargeDate)
            return
        }

        val admission = admissionRepository.findById(admissionId)
            .orElseThrow { ResourceNotFoundException("Admission not found with id: $admissionId") }

        val charge = PatientCharge(
            admission = admission,
            chargeType = ChargeType.DIET,
            description = "Daily Meals",
            quantity = 1,
            unitPrice = mealRate,
            totalAmount = mealRate,
            chargeDate = chargeDate,
        )

        chargeRepository.save(charge)
        log.info("Created DIET charge for admission {} date {}", admissionId, chargeDate)
    }

    @Transactional
    fun createFinalDayCharges(admissionId: Long) {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val today = LocalDate.now()

        // Final-day room charge
        val room = admission.room
        if (room != null && room.price != null) {
            createRoomCharge(admissionId, room, today)
        }

        // Final-day diet charge (only for HOSPITALIZATION)
        if (admission.type == AdmissionType.HOSPITALIZATION) {
            createDietCharge(admissionId, today)
        }
    }

    private fun findActiveAdmission(admissionId: Long): Admission {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")
        if (!admission.isActive()) {
            throw BadRequestException("Cannot add charges to a non-active admission")
        }
        return admission
    }

    private fun validateAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun mapCategoryToChargeType(category: MedicalOrderCategory): ChargeType = when (category) {
        MedicalOrderCategory.LABORATORIOS -> ChargeType.LAB
        MedicalOrderCategory.CUIDADOS_ESPECIALES -> ChargeType.PROCEDURE
        MedicalOrderCategory.REFERENCIAS_MEDICAS -> ChargeType.SERVICE
        MedicalOrderCategory.PRUEBAS_PSICOMETRICAS -> ChargeType.SERVICE
        MedicalOrderCategory.ACTIVIDAD_FISICA -> ChargeType.SERVICE
        else -> ChargeType.SERVICE
    }
}
