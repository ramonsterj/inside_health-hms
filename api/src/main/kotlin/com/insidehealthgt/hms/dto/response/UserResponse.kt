package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val salutation: Salutation?,
    val salutationDisplay: String?,
    val roles: List<String>,
    val permissions: List<String>,
    val status: UserStatus,
    val emailVerified: Boolean,
    val mustChangePassword: Boolean,
    val localePreference: String?,
    val phoneNumbers: List<PhoneNumberResponse>,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            salutation = user.salutation,
            salutationDisplay = user.salutation?.toDisplayString(),
            roles = user.roles.map { it.code },
            permissions = user.getAllPermissions().map { it.code },
            status = user.status,
            emailVerified = user.emailVerified,
            mustChangePassword = user.mustChangePassword,
            localePreference = user.localePreference,
            phoneNumbers = user.phoneNumbers.map { PhoneNumberResponse.from(it) },
            createdAt = user.createdAt,
        )

        private fun Salutation.toDisplayString(): String = when (this) {
            Salutation.SR -> "Sr."
            Salutation.SRA -> "Sra."
            Salutation.SRTA -> "Srta."
            Salutation.DR -> "Dr."
            Salutation.DRA -> "Dra."
            Salutation.MR -> "Mr."
            Salutation.MRS -> "Mrs."
            Salutation.MISS -> "Miss"
        }
    }
}
