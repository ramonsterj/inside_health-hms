package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Patient
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class PatientResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
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
    val notes: String?,
    val hasIdDocument: Boolean,
    val emergencyContacts: List<EmergencyContactResponse>,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(patient: Patient, createdByUser: User? = null, updatedByUser: User? = null): PatientResponse =
            PatientResponse(
                id = patient.id!!,
                firstName = patient.firstName,
                lastName = patient.lastName,
                age = patient.age,
                sex = patient.sex,
                gender = patient.gender,
                maritalStatus = patient.maritalStatus,
                religion = patient.religion,
                educationLevel = patient.educationLevel,
                occupation = patient.occupation,
                address = patient.address,
                email = patient.email,
                idDocumentNumber = patient.idDocumentNumber,
                notes = patient.notes,
                hasIdDocument = patient.hasIdDocument(),
                emergencyContacts = patient.emergencyContacts.map { EmergencyContactResponse.from(it) },
                createdAt = patient.createdAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
                updatedAt = patient.updatedAt,
                updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
            )
    }
}
