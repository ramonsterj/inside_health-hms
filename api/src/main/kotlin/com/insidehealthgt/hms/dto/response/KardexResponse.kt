package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicationAdministration
import com.insidehealthgt.hms.entity.NursingNote
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.VitalSign
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val MINUTES_PER_HOUR = 60.0
private const val NOTE_PREVIEW_LENGTH = 80

private fun User.displayName(): String = listOfNotNull(firstName, lastName).joinToString(" ")

data class KardexAdmissionSummary(
    val admissionId: Long,
    val patientId: Long,
    val patientName: String,
    val roomNumber: String?,
    val triageCode: String?,
    val triageColorCode: String?,
    val admissionType: AdmissionType,
    val admissionDate: LocalDateTime,
    val daysAdmitted: Int,
    val treatingPhysicianName: String,

    val activeMedicationCount: Int,
    val medications: List<KardexMedicationSummary>,

    val activeCareInstructionCount: Int,
    val careInstructions: List<KardexCareInstruction>,

    val latestVitalSigns: KardexVitalSignSummary?,
    val hoursSinceLastVitals: Double?,

    val lastNursingNotePreview: String?,
    val lastNursingNoteAt: LocalDateTime?,
) {
    companion object {
        @Suppress("LongParameterList")
        fun from(
            admission: Admission,
            medications: List<MedicalOrder>,
            careInstructions: List<MedicalOrder>,
            latestVitalSign: VitalSign?,
            latestNursingNote: NursingNote?,
            latestAdministrations: Map<Long, MedicationAdministration>,
            usersById: Map<Long, User>,
        ): KardexAdmissionSummary {
            val daysAdmitted = ChronoUnit.DAYS.between(
                admission.admissionDate.toLocalDate(),
                LocalDate.now(),
            ).toInt()

            val hoursSinceVitals = latestVitalSign?.let {
                ChronoUnit.MINUTES.between(it.recordedAt, LocalDateTime.now()) / MINUTES_PER_HOUR
            }

            val physicianName = admission.treatingPhysician.displayName()

            return KardexAdmissionSummary(
                admissionId = admission.id!!,
                patientId = admission.patient.id!!,
                patientName = listOfNotNull(
                    admission.patient.firstName,
                    admission.patient.lastName,
                ).joinToString(" "),
                roomNumber = admission.room?.number,
                triageCode = admission.triageCode?.code,
                triageColorCode = admission.triageCode?.color,
                admissionType = admission.type,
                admissionDate = admission.admissionDate,
                daysAdmitted = daysAdmitted,
                treatingPhysicianName = physicianName,
                activeMedicationCount = medications.size,
                medications = medications.map {
                    KardexMedicationSummary.from(it, latestAdministrations[it.id], usersById)
                },
                activeCareInstructionCount = careInstructions.size,
                careInstructions = careInstructions.map { KardexCareInstruction.from(it) },
                latestVitalSigns = latestVitalSign?.let { KardexVitalSignSummary.from(it, usersById) },
                hoursSinceLastVitals = hoursSinceVitals,
                lastNursingNotePreview = latestNursingNote?.description?.take(NOTE_PREVIEW_LENGTH),
                lastNursingNoteAt = latestNursingNote?.createdAt,
            )
        }
    }
}

data class KardexMedicationSummary(
    val orderId: Long,
    val medication: String?,
    val dosage: String?,
    val route: AdministrationRoute?,
    val frequency: String?,
    val schedule: String?,
    val inventoryItemId: Long?,
    val inventoryItemName: String?,
    val observations: String?,
    val lastAdministration: KardexLastAdministration?,
) {
    companion object {
        fun from(
            order: MedicalOrder,
            lastAdmin: MedicationAdministration?,
            usersById: Map<Long, User>,
        ): KardexMedicationSummary = KardexMedicationSummary(
            orderId = order.id!!,
            medication = order.medication,
            dosage = order.dosage,
            route = order.route,
            frequency = order.frequency,
            schedule = order.schedule,
            inventoryItemId = order.inventoryItem?.id,
            inventoryItemName = order.inventoryItem?.name,
            observations = order.observations,
            lastAdministration = lastAdmin?.let { KardexLastAdministration.from(it, usersById) },
        )
    }
}

data class KardexLastAdministration(
    val administeredAt: LocalDateTime,
    val status: AdministrationStatus,
    val administeredByName: String?,
) {
    companion object {
        fun from(admin: MedicationAdministration, usersById: Map<Long, User>) = KardexLastAdministration(
            administeredAt = admin.administeredAt,
            status = admin.status,
            administeredByName = admin.createdBy?.let { usersById[it] }?.displayName(),
        )
    }
}

data class KardexCareInstruction(
    val orderId: Long,
    val category: MedicalOrderCategory,
    val startDate: LocalDate,
    val observations: String?,
) {
    companion object {
        fun from(order: MedicalOrder): KardexCareInstruction = KardexCareInstruction(
            orderId = order.id!!,
            category = order.category,
            startDate = order.startDate,
            observations = order.observations,
        )
    }
}

data class KardexVitalSignSummary(
    val recordedAt: LocalDateTime,
    val systolicBp: Int,
    val diastolicBp: Int,
    val heartRate: Int,
    val respiratoryRate: Int,
    val temperature: BigDecimal,
    val oxygenSaturation: Int,
    val recordedByName: String?,
) {
    companion object {
        fun from(vs: VitalSign, usersById: Map<Long, User>) = KardexVitalSignSummary(
            recordedAt = vs.recordedAt,
            systolicBp = vs.systolicBp,
            diastolicBp = vs.diastolicBp,
            heartRate = vs.heartRate,
            respiratoryRate = vs.respiratoryRate,
            temperature = vs.temperature,
            oxygenSaturation = vs.oxygenSaturation,
            recordedByName = vs.createdBy?.let { usersById[it] }?.displayName(),
        )
    }
}
