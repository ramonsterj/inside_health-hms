package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Suppress("TooManyFunctions")
interface AdmissionRepository : JpaRepository<Admission, Long> {

    @Query(
        """
        SELECT DISTINCT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consultingPhysicians cp
        LEFT JOIN FETCH cp.physician
        WHERE a.id = :id
        """,
    )
    fun findByIdWithRelations(@Param("id") id: Long): Admission?

    @Query(
        """
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consentDocument
        """,
        countQuery = "SELECT COUNT(a) FROM Admission a",
    )
    fun findAllWithRelations(pageable: Pageable): Page<Admission>

    @Query(
        """
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consentDocument
        WHERE a.status = :status
        """,
        countQuery = "SELECT COUNT(a) FROM Admission a WHERE a.status = :status",
    )
    fun findAllByStatusWithRelations(@Param("status") status: AdmissionStatus, pageable: Pageable): Page<Admission>

    @Query(
        """
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consentDocument
        WHERE a.type = :type
        """,
        countQuery = "SELECT COUNT(a) FROM Admission a WHERE a.type = :type",
    )
    fun findAllByTypeWithRelations(@Param("type") type: AdmissionType, pageable: Pageable): Page<Admission>

    @Query(
        """
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consentDocument
        WHERE a.status = :status AND a.type = :type
        """,
        countQuery = "SELECT COUNT(a) FROM Admission a WHERE a.status = :status AND a.type = :type",
    )
    fun findAllByStatusAndTypeWithRelations(
        @Param("status") status: AdmissionStatus,
        @Param("type") type: AdmissionType,
        pageable: Pageable,
    ): Page<Admission>

    @Query(
        """
        SELECT DISTINCT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.triageCode
        LEFT JOIN FETCH a.room
        LEFT JOIN FETCH a.treatingPhysician
        LEFT JOIN FETCH a.consentDocument
        LEFT JOIN a.consultingPhysicians cp
        WHERE (a.treatingPhysician.id = :doctorId OR cp.physician.id = :doctorId)
        AND (:status IS NULL OR a.status = :status)
        AND (:type IS NULL OR a.type = :type)
        """,
        countQuery = """
        SELECT COUNT(DISTINCT a) FROM Admission a
        LEFT JOIN a.consultingPhysicians cp
        WHERE (a.treatingPhysician.id = :doctorId OR cp.physician.id = :doctorId)
        AND (:status IS NULL OR a.status = :status)
        AND (:type IS NULL OR a.type = :type)
        """,
    )
    fun findAllByPhysicianWithRelations(
        @Param("doctorId") doctorId: Long,
        @Param("status") status: AdmissionStatus?,
        @Param("type") type: AdmissionType?,
        pageable: Pageable,
    ): Page<Admission>

    fun countByRoomIdAndStatus(roomId: Long, status: AdmissionStatus): Long

    @Query(
        """
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Admission a
        WHERE a.triageCode.id = :triageCodeId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        """,
    )
    fun existsActiveByTriageCodeId(@Param("triageCodeId") triageCodeId: Long): Boolean

    @Query(
        """
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Admission a
        WHERE a.room.id = :roomId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        """,
    )
    fun existsActiveByRoomId(@Param("roomId") roomId: Long): Boolean

    @Query(
        """
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Admission a
        WHERE a.patient.id = :patientId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        """,
    )
    fun existsActiveByPatientId(@Param("patientId") patientId: Long): Boolean

    @Query(
        """
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.room
        WHERE a.status = :status AND a.deletedAt IS NULL
        """,
    )
    fun findAllByStatusWithRoom(@Param("status") status: AdmissionStatus): List<Admission>

    /**
     * Delete all admissions including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM admissions", nativeQuery = true)
    fun deleteAllHard()
}
