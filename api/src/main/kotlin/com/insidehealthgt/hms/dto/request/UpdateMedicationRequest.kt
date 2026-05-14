package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.DosageForm
import com.insidehealthgt.hms.entity.MedicationSection
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateMedicationRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    @field:NotNull
    @field:DecimalMin(value = "0")
    val price: BigDecimal,

    @field:NotNull
    @field:DecimalMin(value = "0")
    val cost: BigDecimal,

    @field:Size(max = 20)
    val sku: String? = null,

    @field:Min(0)
    val restockLevel: Int = 0,

    @field:NotBlank
    @field:Size(max = 150)
    val genericName: String,

    @field:Size(max = 150)
    val commercialName: String? = null,

    @field:Size(max = 50)
    val strength: String? = null,

    @field:NotNull
    val dosageForm: DosageForm,

    val route: AdministrationRoute? = null,

    val controlled: Boolean = false,

    @field:Size(max = 10)
    val atcCode: String? = null,

    @field:NotNull
    val section: MedicationSection,

    val active: Boolean = true,
)
