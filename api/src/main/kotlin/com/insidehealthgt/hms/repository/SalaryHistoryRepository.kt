package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.SalaryHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SalaryHistoryRepository : JpaRepository<SalaryHistory, Long> {

    fun findByEmployeeIdAndEffectiveToIsNull(employeeId: Long): SalaryHistory?

    fun findAllByEmployeeIdOrderByEffectiveFromDesc(employeeId: Long): List<SalaryHistory>
}
