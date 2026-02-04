package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.AdmissionDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AdmissionDocumentRepository : JpaRepository<AdmissionDocument, Long> {

    @Query(
        """
        SELECT ad FROM AdmissionDocument ad
        LEFT JOIN FETCH ad.documentType
        WHERE ad.admission.id = :admissionId
        ORDER BY ad.createdAt DESC
        """,
    )
    fun findByAdmissionIdWithDocumentType(@Param("admissionId") admissionId: Long): List<AdmissionDocument>

    @Query(
        """
        SELECT ad FROM AdmissionDocument ad
        LEFT JOIN FETCH ad.documentType
        WHERE ad.id = :id AND ad.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionIdWithDocumentType(
        @Param("id") id: Long,
        @Param("admissionId") admissionId: Long,
    ): AdmissionDocument?
}
