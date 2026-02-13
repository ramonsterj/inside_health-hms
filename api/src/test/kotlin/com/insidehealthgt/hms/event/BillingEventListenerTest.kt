package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.service.BillingService
import com.insidehealthgt.hms.service.InvoiceService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class BillingEventListenerTest {

    private lateinit var billingService: BillingService
    private lateinit var invoiceService: InvoiceService
    private lateinit var listener: BillingEventListener

    @BeforeEach
    fun setUp() {
        billingService = mock()
        invoiceService = mock()
        listener = BillingEventListener(billingService, invoiceService)
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
    fun `handlePatientDischargedCharges delegates to billingService`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        listener.handlePatientDischargedCharges(event)

        verify(billingService).createFinalDayCharges(10L)
    }

    @Test
    fun `handlePatientDischargedCharges catches exceptions without rethrowing`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        whenever(billingService.createFinalDayCharges(any()))
            .thenThrow(RuntimeException("DB error"))

        listener.handlePatientDischargedCharges(event)

        verify(billingService).createFinalDayCharges(10L)
    }

    @Test
    fun `handlePatientDischargedInvoice delegates to invoiceService`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        listener.handlePatientDischargedInvoice(event)

        verify(invoiceService).generateInvoice(10L)
    }

    @Test
    fun `handlePatientDischargedInvoice catches exceptions without rethrowing`() {
        val event = PatientDischargedEvent(admissionId = 10L, patientId = 1L)

        whenever(invoiceService.generateInvoice(any()))
            .thenThrow(RuntimeException("Invoice error"))

        listener.handlePatientDischargedInvoice(event)

        verify(invoiceService).generateInvoice(10L)
    }
}
