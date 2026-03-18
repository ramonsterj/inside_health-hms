package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.DoctorFeeBillingType
import com.insidehealthgt.hms.entity.DoctorFeeStatus
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateDoctorFeeRequest(

    val patientChargeId: Long? = null,

    @field:NotNull
    val billingType: DoctorFeeBillingType,

    @field:NotNull
    @field:Positive
    val grossAmount: BigDecimal,

    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val commissionPct: BigDecimal? = null,

    @field:NotNull
    val feeDate: LocalDate,

    @field:Size(max = 500)
    val description: String? = null,

    val notes: String? = null,
)

data class UpdateDoctorFeeStatusRequest(

    @field:NotNull
    val status: DoctorFeeStatus,

    @field:Size(max = 100)
    val doctorInvoiceNumber: String? = null,
)

data class SettleDoctorFeeRequest(

    @field:NotNull
    val bankAccountId: Long,

    @field:NotNull
    val paymentDate: LocalDate,

    val notes: String? = null,
)
