package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.RefreshToken
import com.insidehealthgt.hms.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?

    @Modifying
    fun deleteByUser(user: User)

    @Modifying
    fun deleteByExpiresAtBefore(expiresAt: LocalDateTime)
}
