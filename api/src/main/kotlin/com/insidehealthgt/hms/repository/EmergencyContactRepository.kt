package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.EmergencyContact
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface EmergencyContactRepository : JpaRepository<EmergencyContact, Long> {

    /**
     * Delete all emergency contacts including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM emergency_contacts", nativeQuery = true)
    fun deleteAllHard()
}
