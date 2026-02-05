package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.PsychotherapyActivity
import com.insidehealthgt.hms.entity.PsychotherapyCategory
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class PsychotherapyActivityResponse(
    val id: Long,
    val admissionId: Long,
    val category: CategorySummary,
    val description: String,
    val createdAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
) {
    data class CategorySummary(val id: Long, val name: String) {
        companion object {
            fun from(category: PsychotherapyCategory): CategorySummary =
                CategorySummary(id = category.id!!, name = category.name)
        }
    }

    companion object {
        fun from(activity: PsychotherapyActivity, createdByUser: User?): PsychotherapyActivityResponse =
            PsychotherapyActivityResponse(
                id = activity.id!!,
                admissionId = activity.admission.id!!,
                category = CategorySummary.from(activity.category),
                description = activity.description,
                createdAt = activity.createdAt,
                createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            )
    }
}
