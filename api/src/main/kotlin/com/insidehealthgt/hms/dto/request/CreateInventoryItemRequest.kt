package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.entity.TimeUnit
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateInventoryItemRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 150, message = "Name must not exceed 150 characters")
    val name: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:NotNull(message = "Category is required")
    val categoryId: Long,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
    val price: BigDecimal,

    @field:NotNull(message = "Cost is required")
    @field:DecimalMin(value = "0", message = "Cost must be greater than or equal to 0")
    val cost: BigDecimal,

    @field:Min(value = 0, message = "Restock level must be greater than or equal to 0")
    val restockLevel: Int = 0,

    @field:NotNull(message = "Pricing type is required")
    val pricingType: PricingType = PricingType.FLAT,

    val timeUnit: TimeUnit? = null,

    @field:Positive(message = "Time interval must be greater than 0")
    val timeInterval: Int? = null,

    val active: Boolean = true,
)
