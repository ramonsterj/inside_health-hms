package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ClinicalHistory
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class ClinicalHistoryResponse(
    val id: Long,
    val admissionId: Long,
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
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
) {
    companion object {
        fun from(
            clinicalHistory: ClinicalHistory,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ): ClinicalHistoryResponse = ClinicalHistoryResponse(
            id = clinicalHistory.id!!,
            admissionId = clinicalHistory.admission.id!!,
            reasonForAdmission = clinicalHistory.reasonForAdmission,
            historyOfPresentIllness = clinicalHistory.historyOfPresentIllness,
            psychiatricHistory = clinicalHistory.psychiatricHistory,
            medicalHistory = clinicalHistory.medicalHistory,
            familyHistory = clinicalHistory.familyHistory,
            personalHistory = clinicalHistory.personalHistory,
            substanceUseHistory = clinicalHistory.substanceUseHistory,
            legalHistory = clinicalHistory.legalHistory,
            socialHistory = clinicalHistory.socialHistory,
            developmentalHistory = clinicalHistory.developmentalHistory,
            educationalOccupationalHistory = clinicalHistory.educationalOccupationalHistory,
            sexualHistory = clinicalHistory.sexualHistory,
            religiousSpiritualHistory = clinicalHistory.religiousSpiritualHistory,
            mentalStatusExam = clinicalHistory.mentalStatusExam,
            physicalExam = clinicalHistory.physicalExam,
            diagnosticImpression = clinicalHistory.diagnosticImpression,
            treatmentPlan = clinicalHistory.treatmentPlan,
            riskAssessment = clinicalHistory.riskAssessment,
            prognosis = clinicalHistory.prognosis,
            informedConsentNotes = clinicalHistory.informedConsentNotes,
            additionalNotes = clinicalHistory.additionalNotes,
            createdAt = clinicalHistory.createdAt,
            updatedAt = clinicalHistory.updatedAt,
            createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            updatedBy = updatedByUser?.let { MedicalStaffResponse.from(it) },
        )
    }
}
