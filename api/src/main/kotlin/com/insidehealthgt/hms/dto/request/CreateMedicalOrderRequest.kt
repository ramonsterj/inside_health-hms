package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateMedicalOrderRequest(
    @field:NotNull(message = "Category is required")
    val category: MedicalOrderCategory,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    @field:Size(max = 255, message = "Medication must not exceed 255 characters")
    val medication: String? = null,

    @field:Size(max = 100, message = "Dosage must not exceed 100 characters")
    val dosage: String? = null,

    val route: AdministrationRoute? = null,

    @field:Size(max = 100, message = "Frequency must not exceed 100 characters")
    val frequency: String? = null,

    @field:Size(max = 100, message = "Schedule must not exceed 100 characters")
    val schedule: String? = null,

    val observations: String? = null,
)
