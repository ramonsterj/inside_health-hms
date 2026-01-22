package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.PatientIdDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PatientIdDocumentRepository : JpaRepository<PatientIdDocument, Long> {

    fun findByPatientId(patientId: Long): PatientIdDocument?
}
