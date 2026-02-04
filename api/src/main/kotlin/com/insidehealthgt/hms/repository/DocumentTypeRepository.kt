package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.DocumentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DocumentTypeRepository : JpaRepository<DocumentType, Long> {

    fun findByCode(code: String): DocumentType?

    fun findAllByOrderByDisplayOrderAsc(): List<DocumentType>

    fun existsByCode(code: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(dt) > 0 THEN true ELSE false END " +
            "FROM DocumentType dt WHERE dt.code = :code AND dt.id != :excludeId",
    )
    fun existsByCodeExcludingId(@Param("code") code: String, @Param("excludeId") excludeId: Long): Boolean

    @Query(
        """
        SELECT CASE WHEN COUNT(ad) > 0 THEN true ELSE false END
        FROM AdmissionDocument ad
        WHERE ad.documentType.id = :documentTypeId AND ad.deletedAt IS NULL
        """,
    )
    fun hasDocuments(@Param("documentTypeId") documentTypeId: Long): Boolean
}
