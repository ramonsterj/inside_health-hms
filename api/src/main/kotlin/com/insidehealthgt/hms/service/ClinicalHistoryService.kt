package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.request.UpdateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.response.ClinicalHistoryResponse
import com.insidehealthgt.hms.entity.ClinicalHistory
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.ClinicalHistoryRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClinicalHistoryService(
    private val clinicalHistoryRepository: ClinicalHistoryRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun getClinicalHistory(admissionId: Long): ClinicalHistoryResponse? {
        verifyAdmissionExists(admissionId)

        val clinicalHistory = clinicalHistoryRepository.findByAdmissionIdWithRelations(admissionId)
            ?: return null

        return buildResponse(clinicalHistory)
    }

    @Transactional
    fun createClinicalHistory(admissionId: Long, request: CreateClinicalHistoryRequest): ClinicalHistoryResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        if (clinicalHistoryRepository.existsByAdmissionId(admissionId)) {
            throw BadRequestException("Clinical history already exists for this admission")
        }

        val clinicalHistory = ClinicalHistory(
            admission = admission,
            reasonForAdmission = request.reasonForAdmission,
            historyOfPresentIllness = request.historyOfPresentIllness,
            psychiatricHistory = request.psychiatricHistory,
            medicalHistory = request.medicalHistory,
            familyHistory = request.familyHistory,
            personalHistory = request.personalHistory,
            substanceUseHistory = request.substanceUseHistory,
            legalHistory = request.legalHistory,
            socialHistory = request.socialHistory,
            developmentalHistory = request.developmentalHistory,
            educationalOccupationalHistory = request.educationalOccupationalHistory,
            sexualHistory = request.sexualHistory,
            religiousSpiritualHistory = request.religiousSpiritualHistory,
            mentalStatusExam = request.mentalStatusExam,
            physicalExam = request.physicalExam,
            diagnosticImpression = request.diagnosticImpression,
            treatmentPlan = request.treatmentPlan,
            riskAssessment = request.riskAssessment,
            prognosis = request.prognosis,
            informedConsentNotes = request.informedConsentNotes,
            additionalNotes = request.additionalNotes,
        )

        val saved = clinicalHistoryRepository.save(clinicalHistory)
        return buildResponse(saved)
    }

    @Transactional
    fun updateClinicalHistory(admissionId: Long, request: UpdateClinicalHistoryRequest): ClinicalHistoryResponse {
        verifyAdmissionExists(admissionId)

        val clinicalHistory = clinicalHistoryRepository.findByAdmissionIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Clinical history not found for admission: $admissionId")

        clinicalHistory.reasonForAdmission = request.reasonForAdmission
        clinicalHistory.historyOfPresentIllness = request.historyOfPresentIllness
        clinicalHistory.psychiatricHistory = request.psychiatricHistory
        clinicalHistory.medicalHistory = request.medicalHistory
        clinicalHistory.familyHistory = request.familyHistory
        clinicalHistory.personalHistory = request.personalHistory
        clinicalHistory.substanceUseHistory = request.substanceUseHistory
        clinicalHistory.legalHistory = request.legalHistory
        clinicalHistory.socialHistory = request.socialHistory
        clinicalHistory.developmentalHistory = request.developmentalHistory
        clinicalHistory.educationalOccupationalHistory = request.educationalOccupationalHistory
        clinicalHistory.sexualHistory = request.sexualHistory
        clinicalHistory.religiousSpiritualHistory = request.religiousSpiritualHistory
        clinicalHistory.mentalStatusExam = request.mentalStatusExam
        clinicalHistory.physicalExam = request.physicalExam
        clinicalHistory.diagnosticImpression = request.diagnosticImpression
        clinicalHistory.treatmentPlan = request.treatmentPlan
        clinicalHistory.riskAssessment = request.riskAssessment
        clinicalHistory.prognosis = request.prognosis
        clinicalHistory.informedConsentNotes = request.informedConsentNotes
        clinicalHistory.additionalNotes = request.additionalNotes

        val saved = clinicalHistoryRepository.save(clinicalHistory)
        return buildResponse(saved)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun buildResponse(clinicalHistory: ClinicalHistory): ClinicalHistoryResponse {
        val userIds = listOfNotNull(clinicalHistory.createdBy, clinicalHistory.updatedBy).distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return ClinicalHistoryResponse.from(
            clinicalHistory = clinicalHistory,
            createdByUser = clinicalHistory.createdBy?.let { users[it] },
            updatedByUser = clinicalHistory.updatedBy?.let { users[it] },
        )
    }
}
