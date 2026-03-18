package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class GeneratePayrollScheduleRequest(

    @field:NotNull
    @field:Min(GeneratePayrollScheduleRequest.MIN_YEAR)
    @field:Max(GeneratePayrollScheduleRequest.MAX_YEAR)
    val year: Int,
) {
    companion object {
        @Suppress("MagicNumber")
        const val MIN_YEAR: Long = 2000

        @Suppress("MagicNumber")
        const val MAX_YEAR: Long = 2100
    }
}
