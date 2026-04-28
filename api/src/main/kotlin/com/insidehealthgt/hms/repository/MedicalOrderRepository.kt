package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MedicalOrderRepository : JpaRepository<MedicalOrder, Long> {

    @Query(
        """
        SELECT mo FROM MedicalOrder mo
        LEFT JOIN FETCH mo.admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH mo.inventoryItem
        WHERE mo.admission.id = :admissionId
        ORDER BY mo.category, mo.startDate DESC
        """,
    )
    fun findByAdmissionIdWithRelations(@Param("admissionId") admissionId: Long): List<MedicalOrder>

    @Query(
        """
        SELECT mo FROM MedicalOrder mo
        LEFT JOIN FETCH mo.admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH mo.inventoryItem
        WHERE mo.id = :orderId AND mo.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionId(@Param("orderId") orderId: Long, @Param("admissionId") admissionId: Long): MedicalOrder?

    @Query(
        """
        SELECT mo FROM MedicalOrder mo
        LEFT JOIN FETCH mo.inventoryItem
        WHERE mo.admission.id IN :admissionIds
        AND mo.status IN (
            com.insidehealthgt.hms.entity.MedicalOrderStatus.ACTIVA,
            com.insidehealthgt.hms.entity.MedicalOrderStatus.AUTORIZADO
        )
        AND mo.category IN :categories
        ORDER BY mo.category, mo.startDate ASC
        """,
    )
    fun findActiveByAdmissionIdsAndCategories(
        @Param("admissionIds") admissionIds: List<Long>,
        @Param("categories") categories: List<MedicalOrderCategory>,
    ): List<MedicalOrder>

    /**
     * Cross-admission listing for the orders-by-state dashboard.
     * Filters are optional; null/empty means "no restriction on this dimension".
     * Uses LEFT JOIN FETCH for admission + patient + inventoryItem to avoid N+1 in row rendering.
     */
    @Query(
        """
        SELECT mo FROM MedicalOrder mo
        LEFT JOIN FETCH mo.admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH mo.inventoryItem
        WHERE (:statuses IS NULL OR mo.status IN :statuses)
        AND (:categories IS NULL OR mo.category IN :categories)
        """,
        countQuery = """
        SELECT COUNT(mo) FROM MedicalOrder mo
        WHERE (:statuses IS NULL OR mo.status IN :statuses)
        AND (:categories IS NULL OR mo.category IN :categories)
        """,
    )
    fun findByFilters(
        @Param("statuses") statuses: List<MedicalOrderStatus>?,
        @Param("categories") categories: List<MedicalOrderCategory>?,
        pageable: Pageable,
    ): Page<MedicalOrder>
}
