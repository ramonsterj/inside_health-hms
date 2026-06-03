package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicalOrderLabTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Per-order aggregate of lab line items, used by the cross-admission dashboard to render
 * line count + total without an N+1 over individual lines.
 */
interface LabLineAggregate {
    val orderId: Long
    val lineCount: Long
    val total: BigDecimal
}

@Repository
interface MedicalOrderLabTestRepository : JpaRepository<MedicalOrderLabTest, Long> {

    @Query(
        """
        SELECT t.medicalOrder.id AS orderId,
               COUNT(t) AS lineCount,
               COALESCE(SUM(t.salesPrice), 0) AS total
        FROM MedicalOrderLabTest t
        WHERE t.medicalOrder.id IN :orderIds
        GROUP BY t.medicalOrder.id
        """,
    )
    fun aggregateByOrderIds(@Param("orderIds") orderIds: Collection<Long>): List<LabLineAggregate>

    @Query(
        """
        SELECT t FROM MedicalOrderLabTest t
        WHERE t.medicalOrder.id IN :orderIds
        ORDER BY t.id ASC
        """,
    )
    fun findByMedicalOrderIdIn(@Param("orderIds") orderIds: Collection<Long>): List<MedicalOrderLabTest>
}
