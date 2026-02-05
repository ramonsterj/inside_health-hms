package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicalOrder
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
        WHERE mo.id = :orderId AND mo.admission.id = :admissionId
        """,
    )
    fun findByIdAndAdmissionId(@Param("orderId") orderId: Long, @Param("admissionId") admissionId: Long): MedicalOrder?
}
