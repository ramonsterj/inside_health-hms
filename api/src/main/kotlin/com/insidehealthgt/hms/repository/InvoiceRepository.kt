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

    @Query("SELECT i.id, i.invoiceNumber FROM Invoice i WHERE i.id IN :ids")
    fun findInvoiceNumbersByIds(@Param("ids") ids: Collection<Long>): List<Array<Any>>

    @Query(
        """
        SELECT i FROM Invoice i
        LEFT JOIN FETCH i.admission a
        LEFT JOIN FETCH a.patient p
        WHERE (:search = ''
            OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY i.createdAt DESC
        """,
    )
    fun searchInvoices(@Param("search") search: String): List<Invoice>

    @Modifying
    @Transactional
    @Query("DELETE FROM invoices", nativeQuery = true)
    fun deleteAllHard()
}
