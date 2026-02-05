package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.PsychotherapyActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PsychotherapyActivityRepository : JpaRepository<PsychotherapyActivity, Long> {

    @Query(
        "SELECT a FROM PsychotherapyActivity a " +
            "JOIN FETCH a.category " +
            "WHERE a.admission.id = :admissionId " +
            "ORDER BY a.createdAt DESC",
    )
    fun findByAdmissionIdOrderByCreatedAtDesc(admissionId: Long): List<PsychotherapyActivity>

    @Query(
        "SELECT a FROM PsychotherapyActivity a " +
            "JOIN FETCH a.category " +
            "WHERE a.admission.id = :admissionId " +
            "ORDER BY a.createdAt ASC",
    )
    fun findByAdmissionIdOrderByCreatedAtAsc(admissionId: Long): List<PsychotherapyActivity>

    @Query(
        "SELECT a FROM PsychotherapyActivity a " +
            "JOIN FETCH a.category " +
            "WHERE a.id = :id AND a.admission.id = :admissionId",
    )
    fun findByIdAndAdmissionId(id: Long, admissionId: Long): PsychotherapyActivity?

    /**
     * Check if a category is in use by any activities (including soft-deleted ones).
     * Uses native query to bypass the soft delete filter.
     */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM psychotherapy_activities WHERE category_id = :categoryId)",
        nativeQuery = true,
    )
    fun existsByCategoryIdIncludingDeleted(categoryId: Long): Boolean

    /**
     * Delete all activities including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM psychotherapy_activities", nativeQuery = true)
    fun deleteAllHard()
}
