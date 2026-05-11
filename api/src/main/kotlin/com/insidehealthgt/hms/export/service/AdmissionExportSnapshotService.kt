@file:Suppress("ImportOrdering")

package com.insidehealthgt.hms.export.service

import com.insidehealthgt.hms.entity.AdmissionConsentDocument
import com.insidehealthgt.hms.entity.AdmissionDocument
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderDocument
import com.insidehealthgt.hms.entity.MedicationAdministration
import com.insidehealthgt.hms.entity.PatientIdDocument
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.export.dto.AdmissionExportSnapshot
import com.insidehealthgt.hms.export.dto.AdmissionSnapshot
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
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionConsentDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionConsultingPhysicianRepository
import com.insidehealthgt.hms.repository.AdmissionDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.ClinicalHistoryRepository
import com.insidehealthgt.hms.repository.InvoiceRepository
import com.insidehealthgt.hms.repository.MedicalOrderDocumentRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.MedicationAdministrationRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.PatientIdDocumentRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
import com.insidehealthgt.hms.service.MessageService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class AdmissionExportSnapshotService(
    private val admissionRepository: AdmissionRepository,
    private val patientRepository: PatientRepository,
    private val consultingPhysicianRepository: AdmissionConsultingPhysicianRepository,
    private val clinicalHistoryRepository: ClinicalHistoryRepository,
    private val progressNoteRepository: ProgressNoteRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val medicalOrderDocumentRepository: MedicalOrderDocumentRepository,
    private val medicationAdministrationRepository: MedicationAdministrationRepository,
    private val nursingNoteRepository: NursingNoteRepository,
    private val vitalSignRepository: VitalSignRepository,
    private val psychotherapyActivityRepository: PsychotherapyActivityRepository,
    private val patientChargeRepository: PatientChargeRepository,
    private val invoiceRepository: InvoiceRepository,
    private val admissionDocumentRepository: AdmissionDocumentRepository,
    private val admissionConsentDocumentRepository: AdmissionConsentDocumentRepository,
    private val patientIdDocumentRepository: PatientIdDocumentRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {

    /**
     * Fetch every record attached to the admission inside a single read-only transaction
     * with REPEATABLE_READ isolation, then return a detached snapshot of all data.
     * Callers (renderer, appendix builder) must not lazy-load JPA entities afterwards.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Suppress("LongMethod")
    fun fetchSnapshot(
        admissionId: Long,
        generatedAt: LocalDateTime,
        generatedByUserId: Long?,
        generatedByName: String,
    ): AdmissionExportSnapshot {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        val patient = patientRepository.findByIdWithContacts(admission.patient.id!!)
            ?: throw ResourceNotFoundException(messageService.errorPatientNotFound(admission.patient.id!!))

        val consulting = consultingPhysicianRepository
            .findByAdmissionIdWithPhysician(admissionId)
            .map { acp ->
                ConsultingPhysicianSnapshot(
                    physicianName = displayName(acp.physician),
                    reason = acp.reason,
                    requestedDate = acp.requestedDate,
                )
            }

        val clinicalHistory = clinicalHistoryRepository
            .findByAdmissionIdWithRelations(admissionId)
            ?.let { ch ->
                ClinicalHistorySnapshot(
                    createdAt = ch.createdAt,
                    updatedAt = ch.updatedAt,
                    reasonForAdmission = ch.reasonForAdmission,
                    historyOfPresentIllness = ch.historyOfPresentIllness,
                    psychiatricHistory = ch.psychiatricHistory,
                    medicalHistory = ch.medicalHistory,
                    familyHistory = ch.familyHistory,
                    personalHistory = ch.personalHistory,
                    substanceUseHistory = ch.substanceUseHistory,
                    legalHistory = ch.legalHistory,
                    socialHistory = ch.socialHistory,
                    developmentalHistory = ch.developmentalHistory,
                    educationalOccupationalHistory = ch.educationalOccupationalHistory,
                    sexualHistory = ch.sexualHistory,
                    religiousSpiritualHistory = ch.religiousSpiritualHistory,
                    mentalStatusExam = ch.mentalStatusExam,
                    physicalExam = ch.physicalExam,
                    diagnosticImpression = ch.diagnosticImpression,
                    treatmentPlan = ch.treatmentPlan,
                    riskAssessment = ch.riskAssessment,
                    prognosis = ch.prognosis,
                    informedConsentNotes = ch.informedConsentNotes,
                    additionalNotes = ch.additionalNotes,
                )
            }

        val unpaged = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "createdAt"))

        val progressNotes = progressNoteRepository
            .findByAdmissionIdWithRelations(admissionId, unpaged)
            .content
            .sortedBy { it.createdAt }
            .map {
                ProgressNoteSnapshot(
                    id = it.id!!,
                    createdAt = it.createdAt,
                    subjectiveData = it.subjectiveData,
                    objectiveData = it.objectiveData,
                    analysis = it.analysis,
                    actionPlans = it.actionPlans,
                )
            }

        val nursingNotes = nursingNoteRepository
            .findByAdmissionIdWithRelations(admissionId, unpaged)
            .content
            .sortedBy { it.createdAt }
            .map { NursingNoteSnapshot(id = it.id!!, createdAt = it.createdAt, description = it.description) }

        val vitalSigns = vitalSignRepository
            .findByAdmissionIdWithFilters(admissionId, null, null, PageRequest.of(0, MAX_PAGE_SIZE))
            .content
            .sortedBy { it.recordedAt }
            .map {
                VitalSignSnapshot(
                    id = it.id!!,
                    recordedAt = it.recordedAt,
                    systolicBp = it.systolicBp,
                    diastolicBp = it.diastolicBp,
                    heartRate = it.heartRate,
                    respiratoryRate = it.respiratoryRate,
                    temperature = it.temperature,
                    oxygenSaturation = it.oxygenSaturation,
                    glucose = it.glucose,
                    other = it.other,
                )
            }

        val medicalOrders = medicalOrderRepository.findByAdmissionIdWithRelations(admissionId)
        val medicalOrderDocsByOrderId = medicalOrders.associate { order ->
            order.id!! to medicalOrderDocumentRepository.findByMedicalOrderIdOrderByCreatedAtDesc(order.id!!)
        }
        val administrationsByOrderId: Map<Long, List<MedicationAdministration>> = medicalOrders.associate { order ->
            order.id!! to medicationAdministrationRepository.findByOrderIdAndAdmissionId(
                order.id!!,
                admissionId,
                PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "administeredAt")),
            ).content
        }

        val medicalOrderSnapshots = medicalOrders
            .sortedBy { it.createdAt }
            .map { order -> toOrderSnapshot(order, medicalOrderDocsByOrderId, administrationsByOrderId) }

        val medicationAdministrations = medicationAdministrationRepository
            .findByAdmissionIdOrderByAdministeredAtAsc(admissionId)
            .map { toAdministrationSnapshot(it) }

        val psychotherapyActivities = psychotherapyActivityRepository
            .findByAdmissionIdOrderByCreatedAtAsc(admissionId)
            .map {
                PsychotherapyActivitySnapshot(
                    id = it.id!!,
                    createdAt = it.createdAt,
                    categoryName = it.category.name,
                    description = it.description,
                )
            }

        val patientCharges = patientChargeRepository
            .findByAdmissionIdOrderByChargeDateDesc(admissionId)
            .map {
                PatientChargeSnapshot(
                    id = it.id!!,
                    chargeDate = it.chargeDate,
                    chargeType = it.chargeType,
                    description = it.description,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    totalAmount = it.totalAmount,
                    reason = it.reason,
                )
            }

        val invoices = listOfNotNull(invoiceRepository.findByAdmissionId(admissionId))
            .map {
                InvoiceSnapshot(
                    id = it.id!!,
                    invoiceNumber = it.invoiceNumber,
                    totalAmount = it.totalAmount,
                    chargeCount = it.chargeCount,
                    notes = it.notes,
                    createdAt = it.createdAt,
                )
            }

        val emergencyContacts = patient.emergencyContacts
            .map { EmergencyContactSnapshot(it.name, it.relationship, it.phone) }

        val admissionDocs = admissionDocumentRepository.findByAdmissionIdWithDocumentType(admissionId)
        val consentDoc = admissionConsentDocumentRepository.findByAdmissionId(admissionId)
        val patientIdDoc = patientIdDocumentRepository.findByPatientId(patient.id!!)
        val medicalOrderDocs = medicalOrderDocsByOrderId.values.flatten()
        val uploaderNamesById = resolveUserDisplayNames(
            listOfNotNull(consentDoc?.createdBy, patientIdDoc?.createdBy) +
                admissionDocs.mapNotNull { it.createdBy } +
                medicalOrderDocs.mapNotNull { it.createdBy },
        )

        val attachments = buildAttachments(consentDoc, patientIdDoc, admissionDocs, medicalOrderDocs, uploaderNamesById)

        val admissionSnapshot = AdmissionSnapshot(
            id = admission.id!!,
            admissionDate = admission.admissionDate,
            dischargeDate = admission.dischargeDate,
            status = admission.status,
            type = admission.type,
            roomNumber = admission.room?.number,
            roomType = admission.room?.type?.name,
            triageCode = admission.triageCode?.code,
            triageDescription = admission.triageCode?.description,
            treatingPhysicianName = displayName(admission.treatingPhysician),
            inventoryNote = admission.inventory,
        )

        val patientSnapshot = PatientSnapshot(
            id = patient.id!!,
            firstName = patient.firstName,
            lastName = patient.lastName,
            dateOfBirth = patient.dateOfBirth,
            age = derivedAge(patient.dateOfBirth),
            sex = patient.sex,
            gender = patient.gender,
            maritalStatus = patient.maritalStatus,
            religion = patient.religion,
            educationLevel = patient.educationLevel,
            occupation = patient.occupation,
            address = patient.address,
            email = patient.email,
            idDocumentNumber = patient.idDocumentNumber,
        )

        return AdmissionExportSnapshot(
            admission = admissionSnapshot,
            patient = patientSnapshot,
            emergencyContacts = emergencyContacts,
            consultingPhysicians = consulting,
            clinicalHistory = clinicalHistory,
            progressNotes = progressNotes,
            medicalOrders = medicalOrderSnapshots,
            psychotherapyActivities = psychotherapyActivities,
            nursingNotes = nursingNotes,
            vitalSigns = vitalSigns,
            medicationAdministrations = medicationAdministrations,
            patientCharges = patientCharges,
            invoices = invoices,
            attachments = attachments,
            generatedAt = generatedAt,
            generatedByUserId = generatedByUserId,
            generatedByName = generatedByName,
        )
    }

    private fun toOrderSnapshot(
        order: MedicalOrder,
        docsByOrderId: Map<Long, List<MedicalOrderDocument>>,
        administrationsByOrderId: Map<Long, List<MedicationAdministration>>,
    ): MedicalOrderSnapshot {
        val orderId = order.id!!
        return MedicalOrderSnapshot(
            id = orderId,
            createdAt = order.createdAt,
            category = order.category,
            startDate = order.startDate,
            endDate = order.endDate,
            medication = order.medication,
            dosage = order.dosage,
            route = order.route,
            frequency = order.frequency,
            schedule = order.schedule,
            observations = order.observations,
            status = order.status,
            authorizedAt = order.authorizedAt,
            authorizedBy = order.authorizedBy,
            rejectedAt = order.rejectedAt,
            rejectedBy = order.rejectedBy,
            rejectionReason = order.rejectionReason,
            emergencyAuthorized = order.emergencyAuthorized,
            emergencyReason = order.emergencyReason,
            emergencyReasonNote = order.emergencyReasonNote,
            resultsReceivedAt = order.resultsReceivedAt,
            administrations = administrationsByOrderId[orderId].orEmpty().map { toAdministrationSnapshot(it) },
            documentAttachmentIds = docsByOrderId[orderId].orEmpty().mapNotNull { it.id },
        )
    }

    private fun toAdministrationSnapshot(ma: MedicationAdministration): MedicationAdministrationSnapshot =
        MedicationAdministrationSnapshot(
            id = ma.id!!,
            medicalOrderId = ma.medicalOrder.id!!,
            administeredAt = ma.administeredAt,
            status = ma.status,
            notes = ma.notes,
        )

    private fun buildAttachments(
        consentDoc: AdmissionConsentDocument?,
        patientIdDoc: PatientIdDocument?,
        admissionDocs: List<AdmissionDocument>,
        medicalOrderDocs: List<MedicalOrderDocument>,
        uploaderNamesById: Map<Long, String>,
    ): List<AttachmentSnapshot> {
        val result = mutableListOf<AttachmentSnapshot>()
        consentDoc?.let {
            result += AttachmentSnapshot(
                id = it.id!!,
                source = AttachmentSource.CONSENT,
                fileName = it.fileName,
                contentType = it.contentType,
                byteSize = it.fileSize,
                storagePath = it.storagePath,
                uploadedAt = it.createdAt,
                uploadedBy = it.createdBy,
                uploadedByName = it.createdBy?.let(uploaderNamesById::get),
            )
        }
        patientIdDoc?.let {
            result += AttachmentSnapshot(
                id = it.id!!,
                source = AttachmentSource.PATIENT_ID,
                fileName = it.fileName,
                contentType = it.contentType,
                byteSize = it.fileSize,
                storagePath = it.storagePath,
                uploadedAt = it.createdAt,
                uploadedBy = it.createdBy,
                uploadedByName = it.createdBy?.let(uploaderNamesById::get),
            )
        }
        admissionDocs.forEach { doc ->
            result += AttachmentSnapshot(
                id = doc.id!!,
                source = AttachmentSource.ADMISSION_DOCUMENT,
                fileName = doc.displayName.ifBlank { doc.fileName },
                contentType = doc.contentType,
                byteSize = doc.fileSize,
                storagePath = doc.storagePath,
                uploadedAt = doc.createdAt,
                uploadedBy = doc.createdBy,
                uploadedByName = doc.createdBy?.let(uploaderNamesById::get),
                documentTypeName = doc.documentType.name,
            )
        }
        medicalOrderDocs.forEach { doc ->
            result += AttachmentSnapshot(
                id = doc.id!!,
                source = AttachmentSource.MEDICAL_ORDER_DOCUMENT,
                fileName = doc.displayName.ifBlank { doc.fileName },
                contentType = doc.contentType,
                byteSize = doc.fileSize,
                storagePath = doc.storagePath,
                uploadedAt = doc.createdAt,
                uploadedBy = doc.createdBy,
                uploadedByName = doc.createdBy?.let(uploaderNamesById::get),
                medicalOrderId = doc.medicalOrder.id,
            )
        }
        return result
    }

    private fun resolveUserDisplayNames(userIds: Collection<Long>): Map<Long, String> {
        val distinct = userIds.distinct()
        if (distinct.isEmpty()) return emptyMap()
        return userRepository.findAllById(distinct).associate { it.id!! to displayName(it) }
    }

    private fun displayName(user: User): String {
        val first = user.firstName.orEmpty()
        val last = user.lastName.orEmpty()
        val full = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
        return full.ifBlank { user.username }
    }

    private fun derivedAge(dateOfBirth: LocalDate): Int =
        Period.between(dateOfBirth, LocalDate.now(GUATEMALA_ZONE)).years

    companion object {
        private const val MAX_PAGE_SIZE = 10_000
        private val GUATEMALA_ZONE = ZoneId.of("America/Guatemala")
    }
}
