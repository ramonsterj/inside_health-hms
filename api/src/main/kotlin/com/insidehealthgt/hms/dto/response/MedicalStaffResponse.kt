package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User

data class MedicalStaffResponse(
    val id: Long,
    val salutation: String?,
    val firstName: String?,
    val lastName: String?,
    val roles: List<String>,
) {
    companion object {
        fun from(user: User) = MedicalStaffResponse(
            id = user.id!!,
            salutation = user.salutation?.name,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles.map { it.code },
        )
    }
}
