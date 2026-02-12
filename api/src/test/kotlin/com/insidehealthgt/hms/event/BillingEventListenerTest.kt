package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.service.BillingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class BillingEventListenerTest {

    private lateinit var billingService: BillingService
    private lateinit var listener: BillingEventListener

    @BeforeEach
    fun setUp() {
        billingService = mock()
        listener = BillingEventListener(billingService)
    }

    @Test
    fun `handleInventoryDispensed delegates to billingService`() {
        val event = InventoryDispensedEvent(
            admissionId = 10L,
            inventoryItemId = 5L,
            itemName = "Amoxicillin 500mg",
            quantity = 3,
            unitPrice = BigDecimal("25.00"),
        )

        listener.handleInventoryDispensed(event)

        verify(billingService).createChargeFromInventoryDispensed(event)
    }

    @Test
    fun `handleInventoryDispensed catches exceptions without rethrowing`() {
        val event = InventoryDispensedEvent(
            admissionId = 10L,
            inventoryItemId = 5L,
            itemName = "Amoxicillin 500mg",
            quantity = 3,
            unitPrice = BigDecimal("25.00"),
        )

        whenever(billingService.createChargeFromInventoryDispensed(any()))
            .thenThrow(RuntimeException("DB error"))

        listener.handleInventoryDispensed(event)

        verify(billingService).createChargeFromInventoryDispensed(event)
    }

    @Test
    fun `handlePatientDischarged completes without error`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        listener.handlePatientDischarged(event)
    }

    @Test
    fun `handlePatientDischarged does not invoke any billing service methods`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        listener.handlePatientDischarged(event)

        verify(billingService, never()).createChargeFromInventoryDispensed(any())
    }
}
