package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Patient

data class PatientSummaryResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val idDocumentNumber: String?,
    val hasIdDocument: Boolean,
) {
    companion object {
        fun from(patient: Patient, hasIdDocument: Boolean): PatientSummaryResponse = PatientSummaryResponse(
            id = patient.id!!,
            firstName = patient.firstName,
            lastName = patient.lastName,
            age = patient.age,
            idDocumentNumber = patient.idDocumentNumber,
            hasIdDocument = hasIdDocument,
        )
    }
}
