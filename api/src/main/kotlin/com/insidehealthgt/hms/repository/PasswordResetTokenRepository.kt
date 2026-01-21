package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.PasswordResetToken
import com.insidehealthgt.hms.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): PasswordResetToken?

    @Modifying
    fun deleteByUser(user: User)

    @Modifying
    fun deleteByExpiresAtBefore(expiresAt: LocalDateTime)
}
