package com.insidehealthgt.hms.export.service

import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.export.dto.AdmissionExportSnapshot
import com.insidehealthgt.hms.export.dto.AdmissionSnapshot
import com.insidehealthgt.hms.export.dto.AttachmentIndexEntry
import com.insidehealthgt.hms.export.dto.AttachmentSnapshot
import com.insidehealthgt.hms.export.dto.AttachmentSource
import com.insidehealthgt.hms.export.dto.ClinicalHistorySnapshot
import com.insidehealthgt.hms.export.dto.ConsultingPhysicianSnapshot
import com.insidehealthgt.hms.export.dto.EmergencyContactSnapshot
import com.insidehealthgt.hms.export.dto.InvoiceSnapshot
import com.insidehealthgt.hms.export.dto.MedicalOrderSnapshot
import com.insidehealthgt.hms.export.dto.MedicationAdministrationSnapshot
import com.insidehealthgt.hms.export.dto.NursingNoteSnapshot
import com.insidehealthgt.hms.export.dto.PatientChargeSnapshot
import com.insidehealthgt.hms.export.dto.PatientSnapshot
import com.insidehealthgt.hms.export.dto.ProgressNoteSnapshot
import com.insidehealthgt.hms.export.dto.PsychotherapyActivitySnapshot
import com.insidehealthgt.hms.export.dto.VitalSignSnapshot
import com.insidehealthgt.hms.service.MessageService
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdmissionExportRendererTest {

    private val renderer = AdmissionExportRenderer(
        sanitizer = AdmissionExportHtmlSanitizer(),
        messageService = MessageService(StaticMessageSource()),
    )

    @Test
    fun `cover page header embeds the brand logo and running footer is declared`() {
        val html = renderer.buildHtml(snapshot = baseSnapshot(), locale = Locale.ENGLISH)

        // SVG logo from export/logo.svg is inlined (its viewBox attribute is preserved)
        assertTrue(html.contains("viewBox=\"0 0 565.4 228.6\""))
        // Logo appears in cover header
        assertTrue(html.contains("class=\"cover-header\""))
        // Running footer element + page-rule wiring
        assertTrue(html.contains("class=\"page-footer\""))
        assertTrue(html.contains("position: running(pageFooter)"))
        assertTrue(html.contains("counter(page)"))
    }

    @Test
    fun `cover page renders patient and admission identity fields`() {
        val html = renderer.buildHtml(snapshot = baseSnapshot(), locale = Locale.ENGLISH)

        assertTrue(html.contains("Juan"))
        assertTrue(html.contains("Perez"))
        assertTrue(html.contains("Dr. Garcia"))
        assertTrue(html.contains("11/05/1981"))
        assertTrue(html.contains(">10<"))
        assertTrue(html.contains("AMBULATORY"))
        assertTrue(html.contains("09/05/2026 - 14:30"))
    }

    @Test
    fun `demographics section includes emergency contacts when present`() {
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(
                emergencyContacts = listOf(EmergencyContactSnapshot("Maria Perez", "Wife", "555-1234")),
            ),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("Maria Perez"))
        assertTrue(html.contains("Wife"))
        assertTrue(html.contains("555-1234"))
    }

    @Test
    fun `demographics section renders placeholder when contacts are empty`() {
        val html = renderer.buildHtml(snapshot = baseSnapshot(), locale = Locale.ENGLISH)

        assertTrue(html.contains("admission.export.no_records"))
    }

    @Test
    fun `consulting physicians section renders rows or placeholder`() {
        val withPhysician = renderer.buildHtml(
            snapshot = baseSnapshot().copy(
                consultingPhysicians = listOf(
                    ConsultingPhysicianSnapshot("Dr. Smith", "Cardiology consult", LocalDate.of(2026, 5, 10)),
                ),
            ),
            locale = Locale.ENGLISH,
        )
        assertTrue(withPhysician.contains("Dr. Smith"))
        assertTrue(withPhysician.contains("Cardiology consult"))
        assertTrue(withPhysician.contains("10/05/2026"))

        val empty = renderer.buildHtml(snapshot = baseSnapshot(), locale = Locale.ENGLISH)
        assertTrue(empty.contains("admission.export.no_records"))
    }

    @Test
    fun `clinical history renders every section field`() {
        val clinical = ClinicalHistorySnapshot(
            createdAt = null,
            updatedAt = null,
            reasonForAdmission = "Reason text",
            historyOfPresentIllness = "<p>Present illness</p>",
            psychiatricHistory = null,
            medicalHistory = null,
            familyHistory = null,
            personalHistory = null,
            substanceUseHistory = null,
            legalHistory = null,
            socialHistory = null,
            developmentalHistory = null,
            educationalOccupationalHistory = null,
            sexualHistory = null,
            religiousSpiritualHistory = null,
            mentalStatusExam = null,
            physicalExam = null,
            diagnosticImpression = "Major depressive disorder",
            treatmentPlan = null,
            riskAssessment = null,
            prognosis = null,
            informedConsentNotes = null,
            additionalNotes = null,
        )
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(clinicalHistory = clinical),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("Reason text"))
        assertTrue(html.contains("Present illness"))
        assertTrue(html.contains("Major depressive disorder"))
    }

    @Test
    fun `clinical history shows placeholder when null`() {
        val html = renderer.buildHtml(snapshot = baseSnapshot(), locale = Locale.ENGLISH)

        val historyHeader = html.substringAfter("admission.export.sections.clinicalHistory")
        assertTrue(historyHeader.contains("admission.export.no_records"))
    }

    @Test
    fun `progress notes render SOAP fields chronologically`() {
        val notes = listOf(
            ProgressNoteSnapshot(
                id = 1,
                createdAt = LocalDateTime.of(2026, 5, 10, 9, 0),
                subjectiveData = "Patient reports headache",
                objectiveData = "BP 120/80",
                analysis = "Stable",
                actionPlans = "Continue plan",
            ),
            ProgressNoteSnapshot(
                id = 2,
                createdAt = LocalDateTime.of(2026, 5, 11, 9, 0),
                subjectiveData = "Improving",
                objectiveData = "Vitals stable",
                analysis = "Recovery",
                actionPlans = "Plan B",
            ),
        )
        val html = renderer.buildHtml(snapshot = baseSnapshot().copy(progressNotes = notes), locale = Locale.ENGLISH)

        assertTrue(html.contains("Patient reports headache"))
        assertTrue(html.contains("Improving"))
        assertTrue(html.indexOf("Patient reports headache") < html.indexOf("Improving"))
        assertTrue(html.contains("10/05/2026 - 09:00"))
        assertTrue(html.contains("11/05/2026 - 09:00"))
    }

    @Test
    fun `medical orders render grouped by category with administrations`() {
        val orders = listOf(
            MedicalOrderSnapshot(
                id = 1,
                createdAt = LocalDateTime.of(2026, 5, 10, 12, 0),
                category = MedicalOrderCategory.MEDICAMENTOS,
                startDate = LocalDate.of(2026, 5, 10),
                endDate = LocalDate.of(2026, 5, 20),
                medication = "Sertralina",
                dosage = "50 mg",
                route = null,
                frequency = "Daily",
                schedule = null,
                observations = "Take with food",
                status = MedicalOrderStatus.AUTORIZADO,
                authorizedAt = LocalDateTime.of(2026, 5, 10, 12, 30),
                authorizedBy = 1,
                rejectedAt = null,
                rejectedBy = null,
                rejectionReason = null,
                emergencyAuthorized = false,
                emergencyReason = null,
                emergencyReasonNote = null,
                resultsReceivedAt = null,
                administrations = listOf(
                    MedicationAdministrationSnapshot(
                        id = 99,
                        medicalOrderId = 1,
                        administeredAt = LocalDateTime.of(2026, 5, 10, 18, 0),
                        status = AdministrationStatus.GIVEN,
                        notes = "Tolerated well",
                    ),
                ),
                documentAttachmentIds = listOf(5L),
            ),
        )
        val html = renderer.buildHtml(snapshot = baseSnapshot().copy(medicalOrders = orders), locale = Locale.ENGLISH)

        assertTrue(html.contains("MEDICAMENTOS"))
        assertTrue(html.contains("Sertralina"))
        assertTrue(html.contains("50 mg"))
        assertTrue(html.contains("Take with food"))
        assertTrue(html.contains("Tolerated well"))
        assertTrue(html.contains("10/05/2026 - 18:00"))
    }

    @Test
    fun `psychotherapy activities render categorized rows`() {
        val activities = listOf(
            PsychotherapyActivitySnapshot(
                id = 1,
                createdAt = LocalDateTime.of(2026, 5, 10, 10, 0),
                categoryName = "Group Therapy",
                description = "Discussed coping strategies",
            ),
        )
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(psychotherapyActivities = activities),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("Group Therapy"))
        assertTrue(html.contains("Discussed coping strategies"))
    }

    @Test
    fun `nursing notes render with timestamp and body`() {
        val notes = listOf(
            NursingNoteSnapshot(
                id = 1,
                createdAt = LocalDateTime.of(2026, 5, 10, 8, 0),
                description = "Patient ate breakfast",
            ),
        )
        val html = renderer.buildHtml(snapshot = baseSnapshot().copy(nursingNotes = notes), locale = Locale.ENGLISH)

        assertTrue(html.contains("Patient ate breakfast"))
        assertTrue(html.contains("10/05/2026 - 08:00"))
    }

    @Test
    fun `vital signs render as a table with one row per record`() {
        val signs = listOf(
            VitalSignSnapshot(
                id = 1,
                recordedAt = LocalDateTime.of(2026, 5, 10, 6, 0),
                systolicBp = 120,
                diastolicBp = 80,
                heartRate = 70,
                respiratoryRate = 16,
                temperature = BigDecimal("36.5"),
                oxygenSaturation = 98,
                glucose = 95,
                other = "Normal",
            ),
        )
        val html = renderer.buildHtml(snapshot = baseSnapshot().copy(vitalSigns = signs), locale = Locale.ENGLISH)

        assertTrue(html.contains("120/80"))
        assertTrue(html.contains(">70<"))
        assertTrue(html.contains("36.5"))
        assertTrue(html.contains(">98<"))
        assertTrue(html.contains(">95<"))
    }

    @Test
    fun `medication administrations render as a flat list`() {
        val list = listOf(
            MedicationAdministrationSnapshot(
                id = 1,
                medicalOrderId = 7,
                administeredAt = LocalDateTime.of(2026, 5, 10, 18, 30),
                status = AdministrationStatus.GIVEN,
                notes = "Given on schedule",
            ),
        )
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(medicationAdministrations = list),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("GIVEN"))
        assertTrue(html.contains("Given on schedule"))
        assertTrue(html.contains("10/05/2026 - 18:30"))
    }

    @Test
    fun `billing section renders charges and invoices`() {
        val charges = listOf(
            PatientChargeSnapshot(
                id = 1,
                chargeDate = LocalDate.of(2026, 5, 10),
                chargeType = ChargeType.ROOM,
                description = "Daily room charge",
                quantity = 1,
                unitPrice = BigDecimal("250.00"),
                totalAmount = BigDecimal("250.00"),
                reason = null,
            ),
        )
        val invoices = listOf(
            InvoiceSnapshot(
                id = 5,
                invoiceNumber = "INV-001",
                totalAmount = BigDecimal("1500.00"),
                chargeCount = 4,
                notes = "Final invoice",
                createdAt = LocalDateTime.of(2026, 5, 12, 10, 0),
            ),
        )
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(patientCharges = charges, invoices = invoices),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("Daily room charge"))
        assertTrue(html.contains("250.00"))
        assertTrue(html.contains("INV-001"))
        assertTrue(html.contains("1500.00"))
        assertTrue(html.contains("Final invoice"))
    }

    @Test
    fun `documents index renders attachment metadata and appendix page number`() {
        val html = renderer.buildHtml(
            snapshot = snapshotWithAttachment(),
            locale = Locale.ENGLISH,
            attachmentIndexEntries = listOf(
                AttachmentIndexEntry(
                    attachmentId = 7,
                    source = AttachmentSource.ADMISSION_DOCUMENT,
                    checksum = "abc123checksum",
                    relativeAppendixPageNumber = 1,
                    appendixPageNumber = 6,
                ),
            ),
        )

        assertTrue(html.contains("Reception Staff"))
        assertTrue(html.contains("abc123checksum"))
        assertTrue(html.contains("<td>6</td>"))
        assertTrue(html.contains("lab-result.pdf"))
    }

    @Test
    fun `rich text fields are sanitized at render time`() {
        val clinical = ClinicalHistorySnapshot(
            createdAt = null,
            updatedAt = null,
            reasonForAdmission = "<p>Safe</p><script>alert('xss')</script>",
            historyOfPresentIllness = null,
            psychiatricHistory = null,
            medicalHistory = null,
            familyHistory = null,
            personalHistory = null,
            substanceUseHistory = null,
            legalHistory = null,
            socialHistory = null,
            developmentalHistory = null,
            educationalOccupationalHistory = null,
            sexualHistory = null,
            religiousSpiritualHistory = null,
            mentalStatusExam = null,
            physicalExam = null,
            diagnosticImpression = null,
            treatmentPlan = null,
            riskAssessment = null,
            prognosis = null,
            informedConsentNotes = null,
            additionalNotes = null,
        )
        val html = renderer.buildHtml(
            snapshot = baseSnapshot().copy(clinicalHistory = clinical),
            locale = Locale.ENGLISH,
        )

        assertTrue(html.contains("Safe"))
        assertFalse(html.contains("<script"))
        assertFalse(html.contains("alert("))
    }

    private fun baseSnapshot(): AdmissionExportSnapshot = AdmissionExportSnapshot(
        admission = AdmissionSnapshot(
            id = 10,
            admissionDate = LocalDateTime.of(2026, 5, 9, 14, 30),
            dischargeDate = null,
            status = AdmissionStatus.ACTIVE,
            type = AdmissionType.AMBULATORY,
            roomNumber = null,
            roomType = null,
            triageCode = null,
            triageDescription = null,
            treatingPhysicianName = "Dr. Garcia",
            inventoryNote = null,
        ),
        patient = PatientSnapshot(
            id = 20,
            firstName = "Juan",
            lastName = "Perez",
            dateOfBirth = LocalDate.of(1981, 5, 11),
            age = 45,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Catolica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Ingeniero",
            address = "Guatemala City",
            email = "juan.perez@example.com",
            idDocumentNumber = null,
        ),
        emergencyContacts = emptyList(),
        consultingPhysicians = emptyList(),
        clinicalHistory = null,
        progressNotes = emptyList(),
        medicalOrders = emptyList(),
        psychotherapyActivities = emptyList(),
        nursingNotes = emptyList(),
        vitalSigns = emptyList(),
        medicationAdministrations = emptyList(),
        patientCharges = emptyList(),
        invoices = emptyList(),
        attachments = emptyList(),
        generatedAt = LocalDateTime.of(2026, 5, 11, 15, 0),
        generatedByUserId = 3,
        generatedByName = "Reception Staff",
    )

    private fun snapshotWithAttachment(): AdmissionExportSnapshot = baseSnapshot().copy(
        attachments = listOf(
            AttachmentSnapshot(
                id = 7,
                source = AttachmentSource.ADMISSION_DOCUMENT,
                fileName = "lab-result.pdf",
                contentType = "application/pdf",
                byteSize = 1024,
                storagePath = "patients/20/admission/lab-result.pdf",
                uploadedAt = LocalDateTime.of(2026, 5, 10, 9, 0),
                uploadedBy = 3,
                uploadedByName = "Reception Staff",
            ),
        ),
    )
}
