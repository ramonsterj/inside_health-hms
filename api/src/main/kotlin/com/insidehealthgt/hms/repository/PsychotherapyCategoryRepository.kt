package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.PsychotherapyCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PsychotherapyCategoryRepository : JpaRepository<PsychotherapyCategory, Long> {

    fun findAllByOrderByDisplayOrderAsc(): List<PsychotherapyCategory>

    fun findAllByActiveTrueOrderByDisplayOrderAsc(): List<PsychotherapyCategory>

    fun existsByName(name: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM PsychotherapyCategory c WHERE c.name = :name AND c.id != :excludeId",
    )
    fun existsByNameExcludingId(name: String, excludeId: Long): Boolean

    /**
     * Delete all categories including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM psychotherapy_categories", nativeQuery = true)
    fun deleteAllHard()
}
