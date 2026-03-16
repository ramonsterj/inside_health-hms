package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicalOrderDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MedicalOrderDocumentRepository : JpaRepository<MedicalOrderDocument, Long> {

    @Query(
        """
        SELECT d FROM MedicalOrderDocument d
        WHERE d.medicalOrder.id = :orderId
        ORDER BY d.createdAt DESC
        """,
    )
    fun findByMedicalOrderIdOrderByCreatedAtDesc(@Param("orderId") orderId: Long): List<MedicalOrderDocument>

    @Query(
        """
        SELECT d FROM MedicalOrderDocument d
        WHERE d.id = :id AND d.medicalOrder.id = :orderId
        """,
    )
    fun findByIdAndMedicalOrderId(@Param("id") id: Long, @Param("orderId") orderId: Long): MedicalOrderDocument?

    @Query(
        """
        SELECT d.medicalOrder.id, COUNT(d)
        FROM MedicalOrderDocument d
        WHERE d.medicalOrder.id IN :orderIds
        GROUP BY d.medicalOrder.id
        """,
    )
    fun countByMedicalOrderIds(@Param("orderIds") orderIds: List<Long>): List<Array<Any>>
}
