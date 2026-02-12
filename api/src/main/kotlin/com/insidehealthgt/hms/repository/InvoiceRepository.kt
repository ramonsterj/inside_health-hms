package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Invoice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface InvoiceRepository : JpaRepository<Invoice, Long> {

    @Query(
        """
        SELECT i FROM Invoice i
        LEFT JOIN FETCH i.admission a
        LEFT JOIN FETCH a.patient
        WHERE i.admission.id = :admissionId
        """,
    )
    fun findByAdmissionId(@Param("admissionId") admissionId: Long): Invoice?

    fun existsByAdmissionId(admissionId: Long): Boolean

    @Modifying
    @Transactional
    @Query("DELETE FROM invoices", nativeQuery = true)
    fun deleteAllHard()
}
