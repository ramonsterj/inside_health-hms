package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Patient
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
interface PatientRepository : JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.emergencyContacts WHERE p.id = :id")
    fun findByIdWithContacts(id: Long): Patient?

    @Query("SELECT p.id FROM Patient p JOIN p.idDocument d WHERE p.id IN :patientIds")
    fun findPatientIdsWithIdDocument(@Param("patientIds") patientIds: List<Long>): List<Long>

    @Suppress("MaxLineLength", "MaximumLineLength")
    @Query(
        value = "SELECT * FROM patients p WHERE p.deleted_at IS NULL AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        countQuery = "SELECT COUNT(*) FROM patients p WHERE p.deleted_at IS NULL AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        nativeQuery = true,
    )
    fun searchByNameOrDocument(@Param("search") search: String, pageable: Pageable): Page<Patient>

    @Query(
        "SELECT p FROM Patient p WHERE " +
            "(LOWER(p.firstName) = LOWER(:firstName) AND LOWER(p.lastName) = LOWER(:lastName) " +
            "AND p.dateOfBirth = :dateOfBirth) OR " +
            "(p.idDocumentNumber IS NOT NULL AND p.idDocumentNumber = :idDocumentNumber)",
    )
    fun findPotentialDuplicates(
        @Param("firstName") firstName: String,
        @Param("lastName") lastName: String,
        @Param("dateOfBirth") dateOfBirth: LocalDate,
        @Param("idDocumentNumber") idDocumentNumber: String?,
    ): List<Patient>

    @Query(
        "SELECT DISTINCT a.patient.id FROM Admission a " +
            "WHERE a.patient.id IN :patientIds AND a.status = 'ACTIVE' AND a.deletedAt IS NULL",
    )
    fun findPatientIdsWithActiveAdmission(@Param("patientIds") patientIds: List<Long>): List<Long>

    @Suppress("MaxLineLength", "MaximumLineLength")
    @Query(
        value = "SELECT DISTINCT p.* FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "LEFT JOIN admission_consulting_physicians acp ON acp.admission_id = a.id AND acp.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (a.treating_physician_id = :doctorId OR acp.physician_id = :doctorId)",
        countQuery = "SELECT COUNT(DISTINCT p.id) FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "LEFT JOIN admission_consulting_physicians acp ON acp.admission_id = a.id AND acp.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (a.treating_physician_id = :doctorId OR acp.physician_id = :doctorId)",
        nativeQuery = true,
    )
    fun findAllByPhysician(@Param("doctorId") doctorId: Long, pageable: Pageable): Page<Patient>

    @Suppress("MaxLineLength", "MaximumLineLength")
    @Query(
        value = "SELECT DISTINCT p.* FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "LEFT JOIN admission_consulting_physicians acp ON acp.admission_id = a.id AND acp.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (a.treating_physician_id = :doctorId OR acp.physician_id = :doctorId) " +
            "AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        countQuery = "SELECT COUNT(DISTINCT p.id) FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "LEFT JOIN admission_consulting_physicians acp ON acp.admission_id = a.id AND acp.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (a.treating_physician_id = :doctorId OR acp.physician_id = :doctorId) " +
            "AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        nativeQuery = true,
    )
    fun searchByNameOrDocumentForPhysician(
        @Param("search") search: String,
        @Param("doctorId") doctorId: Long,
        pageable: Pageable,
    ): Page<Patient>

    @Query(
        "SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admission a " +
            "LEFT JOIN a.consultingPhysicians cp " +
            "WHERE a.patient.id = :patientId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL " +
            "AND (a.treatingPhysician.id = :doctorId OR cp.physician.id = :doctorId)",
    )
    fun isPatientAssignedToDoctor(@Param("patientId") patientId: Long, @Param("doctorId") doctorId: Long): Boolean

    @Suppress("MaxLineLength", "MaximumLineLength")
    @Query(
        value = "SELECT DISTINCT p.* FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL",
        countQuery = "SELECT COUNT(DISTINCT p.id) FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL",
        nativeQuery = true,
    )
    fun findAllWithActiveAdmission(pageable: Pageable): Page<Patient>

    @Suppress("MaxLineLength", "MaximumLineLength")
    @Query(
        value = "SELECT DISTINCT p.* FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        countQuery = "SELECT COUNT(DISTINCT p.id) FROM patients p " +
            "JOIN admissions a ON a.patient_id = p.id AND a.status = 'ACTIVE' AND a.deleted_at IS NULL " +
            "WHERE p.deleted_at IS NULL " +
            "AND (" +
            "LOWER(unaccent(p.first_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(p.last_name)) LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(unaccent(CONCAT(p.first_name, ' ', p.last_name))) " +
            "LIKE LOWER(unaccent(CONCAT('%', :search, '%'))) ESCAPE '\\' OR " +
            "LOWER(p.id_document_number) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')",
        nativeQuery = true,
    )
    fun searchByNameOrDocumentWithActiveAdmission(@Param("search") search: String, pageable: Pageable): Page<Patient>

    /**
     * Delete all patients including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM patients", nativeQuery = true)
    fun deleteAllHard()
}
