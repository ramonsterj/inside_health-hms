package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.DoctorFeeArrangement
import com.insidehealthgt.hms.entity.EmployeeType
import com.insidehealthgt.hms.entity.TreasuryEmployee
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TreasuryEmployeeResponse(
    val id: Long,
    val fullName: String,
    val employeeType: EmployeeType,
    val taxId: String?,
    val position: String?,
    val baseSalary: BigDecimal?,
    val contractedRate: BigDecimal?,
    val doctorFeeArrangement: DoctorFeeArrangement?,
    val hospitalCommissionPct: BigDecimal,
    val hireDate: LocalDate?,
    val terminationDate: LocalDate?,
    val terminationReason: String?,
    val active: Boolean,
    val userId: Long?,
    val notes: String?,
    val indemnizacionLiability: BigDecimal?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            employee: TreasuryEmployee,
            indemnizacionLiability: BigDecimal? = null,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ): TreasuryEmployeeResponse = TreasuryEmployeeResponse(
            id = employee.id!!,
            fullName = employee.fullName,
            employeeType = employee.employeeType,
            taxId = employee.taxId,
            position = employee.position,
            baseSalary = employee.baseSalary,
            contractedRate = employee.contractedRate,
            doctorFeeArrangement = employee.doctorFeeArrangement,
            hospitalCommissionPct = employee.hospitalCommissionPct,
            hireDate = employee.hireDate,
            terminationDate = employee.terminationDate,
            terminationReason = employee.terminationReason,
            active = employee.active,
            userId = employee.userId,
            notes = employee.notes,
            indemnizacionLiability = indemnizacionLiability,
            createdAt = employee.createdAt,
            updatedAt = employee.updatedAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )
    }
}
