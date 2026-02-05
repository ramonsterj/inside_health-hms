package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.ProgressNote
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProgressNoteRepository : JpaRepository<ProgressNote, Long> {

    @Query(
        """
        SELECT pn FROM ProgressNote pn
        LEFT JOIN FETCH pn.admission a
        LEFT JOIN FETCH a.patient
        WHERE pn.admission.id = :admissionId
        ORDER BY pn.createdAt DESC
        """,
        countQuery = "SELECT COUNT(pn) FROM ProgressNote pn WHERE pn.admission.id = :admissionId",
    )
    fun findByAdmissionIdWithRelations(@Param("admissionId") admissionId: Long, pageable: Pageable): Page<ProgressNote>

    @Query(
        """
        SELECT pn FROM ProgressNote pn
        LEFT JOIN FETCH pn.admission a
        LEFT JOIN FETCH a.patient
        WHERE pn.id = :noteId AND pn.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionId(@Param("noteId") noteId: Long, @Param("admissionId") admissionId: Long): ProgressNote?
}
