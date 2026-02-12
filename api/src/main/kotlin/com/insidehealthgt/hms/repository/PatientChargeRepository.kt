package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.PatientCharge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
interface PatientChargeRepository : JpaRepository<PatientCharge, Long> {

    fun findByAdmissionIdOrderByChargeDateDesc(admissionId: Long): List<PatientCharge>

    @Query(
        """
        SELECT pc FROM PatientCharge pc
        WHERE pc.admission.id = :admissionId AND pc.invoice IS NULL
        ORDER BY pc.chargeDate
        """,
    )
    fun findUnbilledByAdmissionId(@Param("admissionId") admissionId: Long): List<PatientCharge>

    fun findByInvoiceIdOrderByChargeDateAsc(invoiceId: Long): List<PatientCharge>

    fun existsByAdmissionIdAndChargeTypeAndChargeDateAndRoomId(
        admissionId: Long,
        chargeType: ChargeType,
        chargeDate: LocalDate,
        roomId: Long,
    ): Boolean

    @Modifying
    @Transactional
    @Query("DELETE FROM patient_charges", nativeQuery = true)
    fun deleteAllHard()
}
