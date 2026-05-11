@file:Suppress("ImportOrdering")

package com.insidehealthgt.hms.export.dto

import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.EmergencyAuthorizationReason
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.Sex
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Detached, immutable snapshot of an admission read inside a single REPEATABLE_READ
 * transaction. The renderer consumes this graph exclusively — no JPA entities are
 * lazily loaded after the snapshot service returns.
 */
data class AdmissionExportSnapshot(
    val admission: AdmissionSnapshot,
    val patient: PatientSnapshot,
    val emergencyContacts: List<EmergencyContactSnapshot>,
    val consultingPhysicians: List<ConsultingPhysicianSnapshot>,
    val clinicalHistory: ClinicalHistorySnapshot?,
    val progressNotes: List<ProgressNoteSnapshot>,
    val medicalOrders: List<MedicalOrderSnapshot>,
    val psychotherapyActivities: List<PsychotherapyActivitySnapshot>,
    val nursingNotes: List<NursingNoteSnapshot>,
    val vitalSigns: List<VitalSignSnapshot>,
    val medicationAdministrations: List<MedicationAdministrationSnapshot>,
    val patientCharges: List<PatientChargeSnapshot>,
    val invoices: List<InvoiceSnapshot>,
    val attachments: List<AttachmentSnapshot>,
    val generatedAt: LocalDateTime,
    val generatedByUserId: Long?,
    val generatedByName: String,
)

data class AdmissionSnapshot(
    val id: Long,
    val admissionDate: LocalDateTime,
    val dischargeDate: LocalDateTime?,
    val status: AdmissionStatus,
    val type: AdmissionType,
    val roomNumber: String?,
    val roomType: String?,
    val triageCode: String?,
    val triageDescription: String?,
    val treatingPhysicianName: String,
    val inventoryNote: String?,
)

data class PatientSnapshot(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val age: Int,
    val sex: Sex,
    val gender: String,
    val maritalStatus: MaritalStatus,
    val religion: String,
    val educationLevel: EducationLevel,
    val occupation: String,
    val address: String,
    val email: String,
    val idDocumentNumber: String?,
)

data class EmergencyContactSnapshot(val name: String, val relationship: String, val phone: String)

data class ConsultingPhysicianSnapshot(val physicianName: String, val reason: String?, val requestedDate: LocalDate?)

data class ClinicalHistorySnapshot(
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val reasonForAdmission: String?,
    val historyOfPresentIllness: String?,
    val psychiatricHistory: String?,
    val medicalHistory: String?,
    val familyHistory: String?,
    val personalHistory: String?,
    val substanceUseHistory: String?,
    val legalHistory: String?,
    val socialHistory: String?,
    val developmentalHistory: String?,
    val educationalOccupationalHistory: String?,
    val sexualHistory: String?,
    val religiousSpiritualHistory: String?,
    val mentalStatusExam: String?,
    val physicalExam: String?,
    val diagnosticImpression: String?,
    val treatmentPlan: String?,
    val riskAssessment: String?,
    val prognosis: String?,
    val informedConsentNotes: String?,
    val additionalNotes: String?,
)

data class ProgressNoteSnapshot(
    val id: Long,
    val createdAt: LocalDateTime?,
    val subjectiveData: String?,
    val objectiveData: String?,
    val analysis: String?,
    val actionPlans: String?,
)

data class MedicalOrderSnapshot(
    val id: Long,
    val createdAt: LocalDateTime?,
    val category: MedicalOrderCategory,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val medication: String?,
    val dosage: String?,
    val route: AdministrationRoute?,
    val frequency: String?,
    val schedule: String?,
    val observations: String?,
    val status: MedicalOrderStatus,
    val authorizedAt: LocalDateTime?,
    val authorizedBy: Long?,
    val rejectedAt: LocalDateTime?,
    val rejectedBy: Long?,
    val rejectionReason: String?,
    val emergencyAuthorized: Boolean,
    val emergencyReason: EmergencyAuthorizationReason?,
    val emergencyReasonNote: String?,
    val resultsReceivedAt: LocalDateTime?,
    val administrations: List<MedicationAdministrationSnapshot>,
    val documentAttachmentIds: List<Long>,
)

data class MedicationAdministrationSnapshot(
    val id: Long,
    val medicalOrderId: Long,
    val administeredAt: LocalDateTime,
    val status: AdministrationStatus,
    val notes: String?,
)

data class NursingNoteSnapshot(val id: Long, val createdAt: LocalDateTime?, val description: String)

data class VitalSignSnapshot(
    val id: Long,
    val recordedAt: LocalDateTime,
    val systolicBp: Int,
    val diastolicBp: Int,
    val heartRate: Int,
    val respiratoryRate: Int,
    val temperature: BigDecimal,
    val oxygenSaturation: Int,
    val glucose: Int?,
    val other: String?,
)

data class PsychotherapyActivitySnapshot(
    val id: Long,
    val createdAt: LocalDateTime?,
    val categoryName: String,
    val description: String,
)

data class PatientChargeSnapshot(
    val id: Long,
    val chargeDate: LocalDate,
    val chargeType: ChargeType,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val reason: String?,
)

data class InvoiceSnapshot(
    val id: Long,
    val invoiceNumber: String,
    val totalAmount: BigDecimal,
    val chargeCount: Int,
    val notes: String?,
    val createdAt: LocalDateTime?,
)

enum class AttachmentSource {
    CONSENT,
    PATIENT_ID,
    ADMISSION_DOCUMENT,
    MEDICAL_ORDER_DOCUMENT,
}

data class AttachmentSnapshot(
    val id: Long,
    val source: AttachmentSource,
    val fileName: String,
    val contentType: String,
    val byteSize: Long,
    val storagePath: String,
    val uploadedAt: LocalDateTime?,
    val uploadedBy: Long?,
    val uploadedByName: String?,
    val medicalOrderId: Long? = null,
    val documentTypeName: String? = null,
)

data class AttachmentIndexEntry(
    val attachmentId: Long,
    val source: AttachmentSource,
    val checksum: String,
    val relativeAppendixPageNumber: Int,
    val appendixPageNumber: Int? = null,
    val skipped: Boolean = false,
)
