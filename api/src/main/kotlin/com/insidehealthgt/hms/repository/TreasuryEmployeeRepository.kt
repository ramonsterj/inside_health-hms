package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.TreasuryEmployee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TreasuryEmployeeRepository : JpaRepository<TreasuryEmployee, Long> {

    fun findAllByOrderByFullNameAsc(): List<TreasuryEmployee>

    fun findAllByActiveTrueOrderByFullNameAsc(): List<TreasuryEmployee>

    fun findAllByEmployeeTypeOrderByFullNameAsc(employeeType: EmployeeType): List<TreasuryEmployee>

    fun findAllByActiveTrueAndEmployeeTypeOrderByFullNameAsc(employeeType: EmployeeType): List<TreasuryEmployee>
}
