package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PayrollEntryRepository : JpaRepository<PayrollEntry, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM PayrollEntry e WHERE e.id = :id AND e.deletedAt IS NULL")
    fun findByIdForUpdate(id: Long): Optional<PayrollEntry>

    fun findAllByEmployeeIdOrderByYearDescDueDateAsc(employeeId: Long): List<PayrollEntry>

    fun findAllByEmployeeIdAndYearOrderByDueDateAsc(employeeId: Long, year: Int): List<PayrollEntry>

    fun findAllByEmployeeIdAndStatusOrderByDueDateAsc(employeeId: Long, status: PayrollStatus): List<PayrollEntry>

    fun existsByEmployeeIdAndYearAndPeriod(
        employeeId: Long,
        year: Int,
        period: com.insidehealthgt.hms.entity.PayrollPeriod,
    ): Boolean

    fun findAllByStatusOrderByDueDateAsc(status: PayrollStatus): List<PayrollEntry>

    fun findAllByStatusAndDueDateBetween(status: PayrollStatus, from: java.time.LocalDate, to: java.time.LocalDate): List<PayrollEntry>

    fun findAllByStatusAndPaidDateBetween(status: PayrollStatus, from: java.time.LocalDate, to: java.time.LocalDate): List<PayrollEntry>
}
