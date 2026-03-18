package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.DoctorFee
import com.insidehealthgt.hms.entity.DoctorFeeBillingType
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DoctorFeeRepository :
    JpaRepository<DoctorFee, Long>,
    JpaSpecificationExecutor<DoctorFee> {

    fun findByPatientChargeIdAndBillingType(chargeId: Long, billingType: DoctorFeeBillingType): DoctorFee?

    fun findAllByTreasuryEmployeeIdAndStatus(employeeId: Long, status: DoctorFeeStatus): List<DoctorFee>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM DoctorFee f WHERE f.id = :id AND f.deletedAt IS NULL")
    fun findByIdForUpdate(id: Long): Optional<DoctorFee>

    @Query(
        """
        SELECT COUNT(f),
               COALESCE(SUM(f.grossAmount), 0),
               COALESCE(SUM(f.netAmount), 0),
               COALESCE(SUM(CASE WHEN f.status = 'PENDING' THEN 1 ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN f.status = 'INVOICED' THEN 1 ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN 1 ELSE 0 END), 0)
        FROM DoctorFee f
        WHERE f.treasuryEmployee.id = :employeeId
        """,
    )
    fun aggregateSummary(employeeId: Long): List<Array<Any>>
}
