package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateVitalSignRequest(
    val recordedAt: LocalDateTime? = null,

    @field:NotNull(message = "Systolic blood pressure is required")
    @field:Min(value = 60, message = "Systolic blood pressure must be at least 60 mmHg")
    @field:Max(value = 250, message = "Systolic blood pressure must be at most 250 mmHg")
    val systolicBp: Int? = null,

    @field:NotNull(message = "Diastolic blood pressure is required")
    @field:Min(value = 30, message = "Diastolic blood pressure must be at least 30 mmHg")
    @field:Max(value = 150, message = "Diastolic blood pressure must be at most 150 mmHg")
    val diastolicBp: Int? = null,

    @field:NotNull(message = "Heart rate is required")
    @field:Min(value = 20, message = "Heart rate must be at least 20 bpm")
    @field:Max(value = 250, message = "Heart rate must be at most 250 bpm")
    val heartRate: Int? = null,

    @field:NotNull(message = "Respiratory rate is required")
    @field:Min(value = 5, message = "Respiratory rate must be at least 5 breaths/min")
    @field:Max(value = 60, message = "Respiratory rate must be at most 60 breaths/min")
    val respiratoryRate: Int? = null,

    @field:NotNull(message = "Temperature is required")
    @field:DecimalMin(value = "30.0", message = "Temperature must be at least 30.0°C")
    @field:DecimalMax(value = "45.0", message = "Temperature must be at most 45.0°C")
    val temperature: BigDecimal? = null,

    @field:NotNull(message = "Oxygen saturation is required")
    @field:Min(value = 50, message = "Oxygen saturation must be at least 50%")
    @field:Max(value = 100, message = "Oxygen saturation must be at most 100%")
    val oxygenSaturation: Int? = null,

    @field:Size(max = 1000, message = "Other observations must be at most 1000 characters")
    val other: String? = null,
)

typealias UpdateVitalSignRequest = CreateVitalSignRequest
