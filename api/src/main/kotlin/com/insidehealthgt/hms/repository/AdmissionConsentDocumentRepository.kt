package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.AdmissionConsentDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface AdmissionConsentDocumentRepository : JpaRepository<AdmissionConsentDocument, Long> {

    fun findByAdmissionId(admissionId: Long): AdmissionConsentDocument?

    /**
     * Delete all consent documents including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM admission_consent_documents", nativeQuery = true)
    fun deleteAllHard()
}
