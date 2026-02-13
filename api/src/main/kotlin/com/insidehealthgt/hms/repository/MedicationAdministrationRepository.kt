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
}
