package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateWarehouseChargeRequest(
    @field:NotNull(message = "Warehouse is required")
    val warehouseId: Long,

    @field:NotNull(message = "Item is required")
    val itemId: Long,

    /** Optional explicit lot for lot-tracked items; FEFO when omitted. */
    val lotId: Long? = null,

    @field:NotNull(message = "Admission is required")
    val admissionId: Long,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String,

    @field:Size(max = 2000, message = "Notes must not exceed 2000 characters")
    val notes: String? = null,
)
