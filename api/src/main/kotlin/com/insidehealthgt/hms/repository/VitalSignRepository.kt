package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.VitalSign
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
interface VitalSignRepository : JpaRepository<VitalSign, Long> {

    @Query(
        """
        SELECT vs FROM VitalSign vs
        LEFT JOIN FETCH vs.admission a
        LEFT JOIN FETCH a.patient
        WHERE vs.admission.id = :admissionId
        AND (CAST(:fromDate AS date) IS NULL OR CAST(vs.recordedAt AS date) >= :fromDate)
        AND (CAST(:toDate AS date) IS NULL OR CAST(vs.recordedAt AS date) <= :toDate)
        """,
        countQuery = """
            SELECT COUNT(vs) FROM VitalSign vs
            WHERE vs.admission.id = :admissionId
            AND (CAST(:fromDate AS date) IS NULL OR CAST(vs.recordedAt AS date) >= :fromDate)
            AND (CAST(:toDate AS date) IS NULL OR CAST(vs.recordedAt AS date) <= :toDate)
        """,
    )
    fun findByAdmissionIdWithFilters(
        @Param("admissionId") admissionId: Long,
        @Param("fromDate") fromDate: LocalDate?,
        @Param("toDate") toDate: LocalDate?,
        pageable: Pageable,
    ): Page<VitalSign>

    @Query(
        """
        SELECT vs FROM VitalSign vs
        LEFT JOIN FETCH vs.admission a
        LEFT JOIN FETCH a.patient
        WHERE vs.id = :vitalSignId AND vs.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionId(
        @Param("vitalSignId") vitalSignId: Long,
        @Param("admissionId") admissionId: Long,
    ): VitalSign?

    /**
     * Delete all vital signs including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM vital_signs", nativeQuery = true)
    fun deleteAllHard()
}
