package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.NursingNote
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface NursingNoteRepository : JpaRepository<NursingNote, Long> {

    @Query(
        """
        SELECT nn FROM NursingNote nn
        LEFT JOIN FETCH nn.admission a
        LEFT JOIN FETCH a.patient
        WHERE nn.admission.id = :admissionId
        """,
        countQuery = "SELECT COUNT(nn) FROM NursingNote nn WHERE nn.admission.id = :admissionId",
    )
    fun findByAdmissionIdWithRelations(@Param("admissionId") admissionId: Long, pageable: Pageable): Page<NursingNote>

    @Query(
        """
        SELECT nn FROM NursingNote nn
        LEFT JOIN FETCH nn.admission a
        LEFT JOIN FETCH a.patient
        WHERE nn.id = :noteId AND nn.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionId(@Param("noteId") noteId: Long, @Param("admissionId") admissionId: Long): NursingNote?

    /**
     * Delete all nursing notes including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM nursing_notes", nativeQuery = true)
    fun deleteAllHard()
}
