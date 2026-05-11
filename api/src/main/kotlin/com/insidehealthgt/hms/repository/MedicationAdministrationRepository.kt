package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicationAdministration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MedicationAdministrationRepository : JpaRepository<MedicationAdministration, Long> {

    @Query(
        """
        SELECT ma FROM MedicationAdministration ma
        LEFT JOIN FETCH ma.medicalOrder mo
        LEFT JOIN FETCH mo.inventoryItem
        WHERE mo.id = :orderId AND ma.admission.id = :admissionId
        ORDER BY ma.administeredAt DESC
        """,
        countQuery = """
        SELECT COUNT(ma) FROM MedicationAdministration ma
        WHERE ma.medicalOrder.id = :orderId AND ma.admission.id = :admissionId
        """,
    )
    fun findByOrderIdAndAdmissionId(
        @Param("orderId") orderId: Long,
        @Param("admissionId") admissionId: Long,
        pageable: Pageable,
    ): Page<MedicationAdministration>

    @Query(
        value = """
            SELECT * FROM (
                SELECT ma.*, ROW_NUMBER() OVER (
                    PARTITION BY ma.medical_order_id
                    ORDER BY ma.administered_at DESC, ma.id DESC
                ) AS rn
                FROM medication_administrations ma
                WHERE ma.medical_order_id IN (:orderIds) AND ma.deleted_at IS NULL
            ) ranked WHERE rn = 1
        """,
        nativeQuery = true,
    )
    fun findLatestByMedicalOrderIds(@Param("orderIds") orderIds: List<Long>): List<MedicationAdministration>

    @Query(
        """
        SELECT ma FROM MedicationAdministration ma
        LEFT JOIN FETCH ma.medicalOrder
        WHERE ma.admission.id = :admissionId
        ORDER BY ma.administeredAt ASC
        """,
    )
    fun findByAdmissionIdOrderByAdministeredAtAsc(
        @Param("admissionId") admissionId: Long,
    ): List<MedicationAdministration>
}
