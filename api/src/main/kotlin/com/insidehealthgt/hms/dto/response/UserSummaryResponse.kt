package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User

data class UserSummaryResponse(val id: Long, val username: String, val firstName: String?, val lastName: String?) {
    companion object {
        fun from(user: User): UserSummaryResponse = UserSummaryResponse(
            id = user.id!!,
            username = user.username,
            firstName = user.firstName,
            lastName = user.lastName,
        )
    }
}
