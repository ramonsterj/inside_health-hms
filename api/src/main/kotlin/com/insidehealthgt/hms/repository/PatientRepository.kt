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

@Repository
interface PatientRepository : JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.emergencyContacts WHERE p.id = :id")
    fun findByIdWithContacts(id: Long): Patient?

    @Query("SELECT p.id FROM Patient p JOIN p.idDocument d WHERE p.id IN :patientIds")
    fun findPatientIdsWithIdDocument(@Param("patientIds") patientIds: List<Long>): List<Long>

    @Query(
        "SELECT p FROM Patient p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.idDocumentNumber) LIKE LOWER(CONCAT('%', :search, '%'))",
    )
    fun searchByNameOrDocument(@Param("search") search: String, pageable: Pageable): Page<Patient>

    @Query(
        "SELECT p FROM Patient p WHERE " +
            "(LOWER(p.firstName) = LOWER(:firstName) AND LOWER(p.lastName) = LOWER(:lastName) AND p.age = :age) OR " +
            "(p.idDocumentNumber IS NOT NULL AND p.idDocumentNumber = :idDocumentNumber)",
    )
    fun findPotentialDuplicates(
        @Param("firstName") firstName: String,
        @Param("lastName") lastName: String,
        @Param("age") age: Int,
        @Param("idDocumentNumber") idDocumentNumber: String?,
    ): List<Patient>

    /**
     * Delete all patients including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM patients", nativeQuery = true)
    fun deleteAllHard()
}
