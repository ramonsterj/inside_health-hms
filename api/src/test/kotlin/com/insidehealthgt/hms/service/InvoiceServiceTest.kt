package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.Invoice
import com.insidehealthgt.hms.entity.Patient
import com.insidehealthgt.hms.entity.PatientCharge
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.InvoiceRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoiceServiceTest {

    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var chargeRepository: PatientChargeRepository
    private lateinit var admissionRepository: AdmissionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var invoiceService: InvoiceService

    private lateinit var testPatient: Patient
    private lateinit var testAdmission: Admission

    @BeforeEach
    fun setUp() {
        invoiceRepository = mock()
        chargeRepository = mock()
        admissionRepository = mock()
        userRepository = mock()

        invoiceService = InvoiceService(invoiceRepository, chargeRepository, admissionRepository, userRepository)

        testPatient = mock<Patient>().apply {
            whenever(id).thenReturn(1L)
            whenever(firstName).thenReturn("Juan")
            whenever(lastName).thenReturn("Perez")
        }

        testAdmission = mock<Admission>().apply {
            whenever(id).thenReturn(10L)
            whenever(patient).thenReturn(testPatient)
            whenever(admissionDate).thenReturn(LocalDateTime.of(2026, 2, 1, 10, 0))
            whenever(dischargeDate).thenReturn(LocalDateTime.of(2026, 2, 7, 14, 0))
            whenever(isDischarged()).thenReturn(true)
            whenever(status).thenReturn(AdmissionStatus.DISCHARGED)
        }
    }

    @Test
    fun `generateInvoice should create invoice for discharged admission`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(invoiceRepository.existsByAdmissionId(10L)).thenReturn(false)

        val charges = listOf(
            createTestCharge(1L, ChargeType.ROOM, BigDecimal("500.00")),
            createTestCharge(2L, ChargeType.MEDICATION, BigDecimal("75.00")),
        )
        whenever(chargeRepository.findUnbilledByAdmissionId(10L)).thenReturn(charges)

        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { invocation ->
            (invocation.arguments[0] as Invoice).apply { id = 1L }
        }
        whenever(chargeRepository.saveAll(any<List<PatientCharge>>())).thenReturn(charges)

        val result = invoiceService.generateInvoice(10L)

        assertEquals(BigDecimal("575.00"), result.totalAmount)
        assertEquals(2, result.chargeCount)
        assertTrue(result.invoiceNumber.startsWith("INV-"))
    }

    @Test
    fun `generateInvoice for active admission should throw`() {
        whenever(testAdmission.isDischarged()).thenReturn(false)
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)

        assertThrows<BadRequestException> {
            invoiceService.generateInvoice(10L)
        }
    }

    @Test
    fun `generateInvoice when invoice already exists should throw 409`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(invoiceRepository.existsByAdmissionId(10L)).thenReturn(true)

        assertThrows<ConflictException> {
            invoiceService.generateInvoice(10L)
        }
    }

    @Test
    fun `generateInvoice with no charges should create empty invoice`() {
        whenever(admissionRepository.findByIdWithRelations(10L)).thenReturn(testAdmission)
        whenever(invoiceRepository.existsByAdmissionId(10L)).thenReturn(false)
        whenever(chargeRepository.findUnbilledByAdmissionId(10L)).thenReturn(emptyList())
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { invocation ->
            (invocation.arguments[0] as Invoice).apply { id = 1L }
        }

        val result = invoiceService.generateInvoice(10L)

        assertEquals(BigDecimal.ZERO, result.totalAmount)
        assertEquals(0, result.chargeCount)
    }

    @Test
    fun `getInvoice should return invoice with charge summary`() {
        val invoice = Invoice(
            invoiceNumber = "INV-2026-0001",
            admission = testAdmission,
            totalAmount = BigDecimal("575.00"),
            chargeCount = 2,
        )
        invoice.id = 1L

        whenever(invoiceRepository.findByAdmissionId(10L)).thenReturn(invoice)

        val charges = listOf(
            createTestChargeWithInvoice(1L, ChargeType.ROOM, BigDecimal("500.00"), invoice),
            createTestChargeWithInvoice(2L, ChargeType.MEDICATION, BigDecimal("75.00"), invoice),
        )
        whenever(chargeRepository.findByInvoiceIdOrderByChargeDateAsc(1L)).thenReturn(charges)

        val result = invoiceService.getInvoice(10L)

        assertEquals("INV-2026-0001", result.invoiceNumber)
        assertEquals(BigDecimal("575.00"), result.totalAmount)
        assertEquals(2, result.chargeSummary.size)
    }

    @Test
    fun `getInvoice for nonexistent invoice should throw 404`() {
        whenever(invoiceRepository.findByAdmissionId(10L)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            invoiceService.getInvoice(10L)
        }
    }

    private fun createTestCharge(id: Long, type: ChargeType, amount: BigDecimal): PatientCharge {
        val charge = PatientCharge(
            admission = testAdmission,
            chargeType = type,
            description = "Test charge",
            quantity = 1,
            unitPrice = amount,
            totalAmount = amount,
            chargeDate = LocalDate.of(2026, 2, 1),
        )
        charge.id = id
        return charge
    }

    private fun createTestChargeWithInvoice(
        id: Long,
        type: ChargeType,
        amount: BigDecimal,
        invoice: Invoice,
    ): PatientCharge {
        val charge = createTestCharge(id, type, amount)
        charge.invoice = invoice
        return charge
    }
}
