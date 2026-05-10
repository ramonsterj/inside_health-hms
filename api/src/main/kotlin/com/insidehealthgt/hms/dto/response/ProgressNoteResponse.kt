package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ProgressNote
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class ProgressNoteResponse(
    val id: Long,
    val admissionId: Long,
    val subjectiveData: String?,
    val objectiveData: String?,
    val analysis: String?,
    val actionPlans: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
    val canEdit: Boolean,
) {
    companion object {
        fun from(
            progressNote: ProgressNote,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            canEdit: Boolean = false,
        ): ProgressNoteResponse = ProgressNoteResponse(
            id = progressNote.id!!,
            admissionId = progressNote.admission.id!!,
            subjectiveData = progressNote.subjectiveData,
            objectiveData = progressNote.objectiveData,
            analysis = progressNote.analysis,
            actionPlans = progressNote.actionPlans,
            createdAt = progressNote.createdAt,
            updatedAt = progressNote.updatedAt,
            createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            updatedBy = updatedByUser?.let { MedicalStaffResponse.from(it) },
            canEdit = canEdit,
        )
    }
}
