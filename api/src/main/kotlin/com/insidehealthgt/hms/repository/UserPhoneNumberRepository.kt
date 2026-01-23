package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.UserPhoneNumber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPhoneNumberRepository : JpaRepository<UserPhoneNumber, Long> {
    fun findByUserId(userId: Long): List<UserPhoneNumber>
}
