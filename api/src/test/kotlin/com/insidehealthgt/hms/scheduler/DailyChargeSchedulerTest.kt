package com.insidehealthgt.hms.scheduler

import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.service.BillingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class DailyChargeSchedulerTest {

    private lateinit var admissionRepository: AdmissionRepository
    private lateinit var billingService: BillingService
    private lateinit var scheduler: DailyChargeScheduler

    @BeforeEach
    fun setUp() {
        admissionRepository = mock()
        billingService = mock()
        scheduler = DailyChargeScheduler(admissionRepository, billingService)
    }

    @Test
    fun `should create room charges for active admissions with rooms`() {
        val room = Room(number = "101", type = RoomType.PRIVATE, gender = RoomGender.MALE, price = BigDecimal("500.00"))
        room.id = 1L

        val admission = mock<Admission>().apply {
            whenever(id).thenReturn(10L)
            whenever(this.room).thenReturn(room)
        }

        whenever(admissionRepository.findAllByStatusWithRoom(AdmissionStatus.ACTIVE))
            .thenReturn(listOf(admission))

        scheduler.generateDailyCharges()

        verify(billingService).createRoomCharge(eq(10L), eq(room), any())
    }

    @Test
    fun `should skip admissions without room`() {
        val admission = mock<Admission>().apply {
            whenever(id).thenReturn(10L)
            whenever(room).thenReturn(null)
        }

        whenever(admissionRepository.findAllByStatusWithRoom(AdmissionStatus.ACTIVE))
            .thenReturn(listOf(admission))

        scheduler.generateDailyCharges()

        verify(billingService, never()).createRoomCharge(any(), any(), any())
    }

    @Test
    fun `should skip rooms without price`() {
        val room = Room(number = "102", type = RoomType.SHARED, gender = RoomGender.MALE, price = null)
        room.id = 2L

        val admission = mock<Admission>().apply {
            whenever(id).thenReturn(11L)
            whenever(this.room).thenReturn(room)
        }

        whenever(admissionRepository.findAllByStatusWithRoom(AdmissionStatus.ACTIVE))
            .thenReturn(listOf(admission))

        scheduler.generateDailyCharges()

        verify(billingService, never()).createRoomCharge(any(), any(), any())
    }

    @Test
    fun `should continue processing after individual failure`() {
        val room1 = Room(
            number = "101",
            type = RoomType.PRIVATE,
            gender = RoomGender.MALE,
            price = BigDecimal("500.00"),
        )
        room1.id = 1L
        val room2 = Room(
            number = "102",
            type = RoomType.SHARED,
            gender = RoomGender.MALE,
            price = BigDecimal("300.00"),
        )
        room2.id = 2L

        val admission1 = mock<Admission>().apply {
            whenever(id).thenReturn(10L)
            whenever(room).thenReturn(room1)
        }
        val admission2 = mock<Admission>().apply {
            whenever(id).thenReturn(11L)
            whenever(room).thenReturn(room2)
        }

        whenever(admissionRepository.findAllByStatusWithRoom(AdmissionStatus.ACTIVE))
            .thenReturn(listOf(admission1, admission2))

        whenever(billingService.createRoomCharge(eq(10L), eq(room1), any()))
            .thenThrow(RuntimeException("DB error"))

        scheduler.generateDailyCharges()

        // Second admission should still be processed despite first failing
        verify(billingService).createRoomCharge(eq(11L), eq(room2), any())
    }
}
