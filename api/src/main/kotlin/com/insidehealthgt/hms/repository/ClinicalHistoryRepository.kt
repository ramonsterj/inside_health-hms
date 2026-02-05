package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.ClinicalHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClinicalHistoryRepository : JpaRepository<ClinicalHistory, Long> {

    @Query(
        """
        SELECT ch FROM ClinicalHistory ch
        LEFT JOIN FETCH ch.admission a
        LEFT JOIN FETCH a.patient
        WHERE ch.admission.id = :admissionId
        """,
    )
    fun findByAdmissionIdWithRelations(@Param("admissionId") admissionId: Long): ClinicalHistory?

    fun existsByAdmissionId(admissionId: Long): Boolean
}
