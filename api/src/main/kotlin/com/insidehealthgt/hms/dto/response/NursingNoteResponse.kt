package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.NursingNote
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class NursingNoteResponse(
    val id: Long,
    val admissionId: Long,
    val description: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
    val canEdit: Boolean,
) {
    companion object {
        fun from(
            nursingNote: NursingNote,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            canEdit: Boolean = false,
        ): NursingNoteResponse = NursingNoteResponse(
            id = nursingNote.id!!,
            admissionId = nursingNote.admission.id!!,
            description = nursingNote.description,
            createdAt = nursingNote.createdAt,
            updatedAt = nursingNote.updatedAt,
            createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            updatedBy = updatedByUser?.let { MedicalStaffResponse.from(it) },
            canEdit = canEdit,
        )
    }
}
