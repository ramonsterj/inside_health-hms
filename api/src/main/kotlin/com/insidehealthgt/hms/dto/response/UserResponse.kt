package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val roles: List<String>,
    val permissions: List<String>,
    val status: UserStatus,
    val emailVerified: Boolean,
    val localePreference: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles.map { it.code },
            permissions = user.getAllPermissions().map { it.code },
            status = user.status,
            emailVerified = user.emailVerified,
            localePreference = user.localePreference,
            createdAt = user.createdAt,
        )
    }
}
