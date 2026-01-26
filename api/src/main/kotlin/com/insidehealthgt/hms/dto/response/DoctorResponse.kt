package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User

data class DoctorResponse(
    val id: Long,
    val salutation: String?,
    val firstName: String?,
    val lastName: String?,
    val username: String,
) {
    companion object {
        fun from(user: User) = DoctorResponse(
            id = user.id!!,
            salutation = user.salutation?.name,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.username,
        )
    }
}
