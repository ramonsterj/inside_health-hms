package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateAdjustmentRequest
import com.insidehealthgt.hms.dto.request.CreateChargeRequest
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.InventoryCategory
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.Patient
import com.insidehealthgt.hms.entity.PatientCharge
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.event.InventoryDispensedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class BillingServiceTest {

    private lateinit var chargeRepository: PatientChargeRepository
    private lateinit var admissionRepository: AdmissionRepository
    private lateinit var inventoryItemRepository: InventoryItemRepository
    private lateinit var userRepository: UserRepository
    private lateinit var billingService: BillingService

    private lateinit var testPatient: Patient
    private lateinit var testAdmission: Admission
    private lateinit var testDoctor: User

    @BeforeEach
    fun setUp() {
        chargeRepository = mock()
        admissionRepository = mock()
        inventoryItemRepository = mock()
        userRepository = mock()

        billingService = BillingService(chargeRepository, admissionRepository, inventoryItemRepository, userRepository)

        testPatient = mock<Patient>().apply {
            whenever(id).thenReturn(1L)
            whenever(firstName).thenReturn("Juan")
            whenever(lastName).thenReturn("Perez")
        }

        testDoctor = mock<User>().apply {
            whenever(id).thenReturn(2L)
        }

        testAdmission = mock<Admission>().apply {
            whenever(id).thenReturn(10L)
            whenever(patient).thenReturn(testPatient)
            whenever(admissionDate).thenReturn(LocalDateTime.of(2026, 2, 1, 10, 0))
            whenever(isActive()).thenReturn(true)
            whenever(isDischarged()).thenReturn(false)
            whenever(status).thenReturn(AdmissionStatus.ACTIVE)
        }
    }

    @Test
    fun `createManualCharge should create charge with correct total`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(chargeRepository.save(any<PatientCharge>())).thenAnswer { invocation ->
            (invocation.arguments[0] as PatientCharge).apply { id = 1L }
        }

        val request = CreateChargeRequest(
            chargeType = ChargeType.SERVICE,
            description = "Therapy session",
            quantity = 2,
            unitPrice = BigDecimal("75.00"),
        )

        val result = billingService.createManualCharge(10L, request)

        assertEquals(BigDecimal("150.00"), result.totalAmount)
        assertEquals(ChargeType.SERVICE, result.chargeType)
        verify(chargeRepository).save(any<PatientCharge>())
    }

    @Test
    fun `createManualCharge on discharged admission should throw`() {
        whenever(testAdmission.isActive()).thenReturn(false)
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        val request = CreateChargeRequest(
            chargeType = ChargeType.SERVICE,
            description = "Test",
            quantity = 1,
            unitPrice = BigDecimal("50.00"),
        )

        assertThrows<BadRequestException> {
            billingService.createManualCharge(10L, request)
        }
    }

    @Test
    fun `createManualCharge on nonexistent admission should throw`() {
        whenever(admissionRepository.findByIdWithRelations(999L)).thenReturn(null)

        val request = CreateChargeRequest(
            chargeType = ChargeType.SERVICE,
            description = "Test",
            quantity = 1,
            unitPrice = BigDecimal("50.00"),
        )

        assertThrows<ResourceNotFoundException> {
            billingService.createManualCharge(999L, request)
        }
    }

    @Test
    fun `createAdjustment should create ADJUSTMENT charge`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(chargeRepository.save(any<PatientCharge>())).thenAnswer { invocation ->
            (invocation.arguments[0] as PatientCharge).apply { id = 2L }
        }

        val request = CreateAdjustmentRequest(
            description = "Billing correction",
            amount = BigDecimal("-75.00"),
            reason = "Duplicate charge",
        )

        val result = billingService.createAdjustment(10L, request)

        assertEquals(ChargeType.ADJUSTMENT, result.chargeType)
        assertEquals(BigDecimal("-75.00"), result.unitPrice)
        assertEquals(BigDecimal("-75.00"), result.totalAmount)
        assertEquals("Duplicate charge", result.reason)
    }

    @Test
    fun `createManualCharge with ROOM type should throw`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        val request = CreateChargeRequest(
            chargeType = ChargeType.ROOM,
            description = "Room charge",
            quantity = 1,
            unitPrice = BigDecimal("500.00"),
        )

        assertThrows<BadRequestException> {
            billingService.createManualCharge(10L, request)
        }
    }

    @Test
    fun `createManualCharge with ADJUSTMENT type should throw`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        val request = CreateChargeRequest(
            chargeType = ChargeType.ADJUSTMENT,
            description = "Adjustment",
            quantity = 1,
            unitPrice = BigDecimal("50.00"),
        )

        assertThrows<BadRequestException> {
            billingService.createManualCharge(10L, request)
        }
    }

    @Test
    fun `getBalance with no charges should return zero`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(chargeRepository.findByAdmissionIdOrderByChargeDateDesc(10L)).thenReturn(emptyList())

        val balance = billingService.getBalance(10L)

        assertEquals(BigDecimal.ZERO, balance.totalBalance)
        assertEquals(0, balance.dailyBreakdown.size)
    }

    @Test
    fun `getBalance should group charges by date`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        val charges = listOf(
            createTestCharge(1L, ChargeType.ROOM, BigDecimal("500.00"), LocalDate.of(2026, 2, 1)),
            createTestCharge(2L, ChargeType.MEDICATION, BigDecimal("75.00"), LocalDate.of(2026, 2, 1)),
            createTestCharge(3L, ChargeType.ROOM, BigDecimal("500.00"), LocalDate.of(2026, 2, 2)),
        )
        whenever(chargeRepository.findByAdmissionIdOrderByChargeDateDesc(10L)).thenReturn(charges)

        val balance = billingService.getBalance(10L)

        assertEquals(BigDecimal("1075.00"), balance.totalBalance)
        assertEquals(2, balance.dailyBreakdown.size)
    }

    @Test
    fun `createChargeFromInventoryDispensed should create MEDICATION charge`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        val category = InventoryCategory(name = "Meds")
        category.id = 1L
        val item = InventoryItem(
            category = category,
            name = "Amoxicillin",
            price = BigDecimal("25.00"),
            cost = BigDecimal("10.00"),
            quantity = 100,
            restockLevel = 10,
        )
        item.id = 5L
        whenever(inventoryItemRepository.findById(5L)).thenReturn(Optional.of(item))
        whenever(chargeRepository.save(any<PatientCharge>())).thenAnswer { invocation ->
            (invocation.arguments[0] as PatientCharge).apply { id = 3L }
        }

        val event = InventoryDispensedEvent(
            admissionId = 10L,
            inventoryItemId = 5L,
            itemName = "Amoxicillin",
            quantity = 3,
            unitPrice = BigDecimal("25.00"),
        )

        billingService.createChargeFromInventoryDispensed(event)

        verify(chargeRepository).save(any<PatientCharge>())
    }

    @Test
    fun `createRoomCharge should skip if already exists`() {
        whenever(
            chargeRepository.existsByAdmissionIdAndChargeTypeAndChargeDateAndRoomId(
                10L,
                ChargeType.ROOM,
                LocalDate.of(2026, 2, 1),
                1L,
            ),
        ).thenReturn(true)

        val room = Room(number = "101", type = RoomType.PRIVATE, gender = RoomGender.MALE, price = BigDecimal("500.00"))
        room.id = 1L

        billingService.createRoomCharge(10L, room, LocalDate.of(2026, 2, 1))

        // Should not attempt to save
        org.mockito.kotlin.verify(chargeRepository, org.mockito.kotlin.never()).save(any<PatientCharge>())
    }

    private fun createTestCharge(id: Long, type: ChargeType, amount: BigDecimal, date: LocalDate): PatientCharge {
        val charge = PatientCharge(
            admission = testAdmission,
            chargeType = type,
            description = "Test charge",
            quantity = 1,
            unitPrice = amount,
            totalAmount = amount,
            chargeDate = date,
        )
        charge.id = id
        return charge
    }
}
