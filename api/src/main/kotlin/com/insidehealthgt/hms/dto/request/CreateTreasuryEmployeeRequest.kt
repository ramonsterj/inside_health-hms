package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.DoctorFeeArrangement
import com.insidehealthgt.hms.entity.EmployeeType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTreasuryEmployeeRequest(

    @field:NotBlank
    @field:Size(max = 255)
    val fullName: String,

    @field:NotNull
    val employeeType: EmployeeType,

    @field:Size(max = 50)
    val taxId: String? = null,

    @field:Size(max = 100)
    val position: String? = null,

    @field:Positive
    val baseSalary: BigDecimal? = null,

    @field:Positive
    val contractedRate: BigDecimal? = null,

    val doctorFeeArrangement: DoctorFeeArrangement? = null,

    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val hospitalCommissionPct: BigDecimal? = null,

    val hireDate: LocalDate? = null,

    val userId: Long? = null,

    val notes: String? = null,
)
