package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.TriageCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TriageCodeRepository : JpaRepository<TriageCode, Long> {

    fun findAllByOrderByDisplayOrderAsc(): List<TriageCode>

    fun existsByCode(code: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM TriageCode t WHERE t.code = :code AND t.id != :excludeId",
    )
    fun existsByCodeExcludingId(code: String, excludeId: Long): Boolean

    /**
     * Delete all triage codes including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM triage_codes", nativeQuery = true)
    fun deleteAllHard()
}
