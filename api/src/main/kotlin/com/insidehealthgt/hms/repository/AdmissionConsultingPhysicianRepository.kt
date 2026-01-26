package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.AdmissionConsultingPhysician
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface AdmissionConsultingPhysicianRepository : JpaRepository<AdmissionConsultingPhysician, Long> {

    fun findByAdmissionId(admissionId: Long): List<AdmissionConsultingPhysician>

    @Query(
        """
        SELECT acp FROM AdmissionConsultingPhysician acp
        LEFT JOIN FETCH acp.physician
        WHERE acp.admission.id = :admissionId
        """,
    )
    fun findByAdmissionIdWithPhysician(@Param("admissionId") admissionId: Long): List<AdmissionConsultingPhysician>

    fun findByIdAndAdmissionId(id: Long, admissionId: Long): AdmissionConsultingPhysician?

    @Query(
        """
        SELECT CASE WHEN COUNT(acp) > 0 THEN true ELSE false END
        FROM AdmissionConsultingPhysician acp
        WHERE acp.admission.id = :admissionId AND acp.physician.id = :physicianId AND acp.deletedAt IS NULL
        """,
    )
    fun existsByAdmissionIdAndPhysicianIdAndNotDeleted(
        @Param("admissionId") admissionId: Long,
        @Param("physicianId") physicianId: Long,
    ): Boolean

    /**
     * Delete all consulting physician records including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM admission_consulting_physicians", nativeQuery = true)
    fun deleteAllHard()
}
