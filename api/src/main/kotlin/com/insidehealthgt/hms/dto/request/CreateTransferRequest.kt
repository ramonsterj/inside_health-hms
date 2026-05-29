package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateTransferRequest(
    @field:NotNull(message = "Source warehouse is required")
    val sourceWarehouseId: Long,

    @field:NotNull(message = "Destination warehouse is required")
    val destinationWarehouseId: Long,

    @field:NotNull(message = "Item is required")
    val itemId: Long,

    /** Required when the item is lot-tracked. */
    val lotId: Long? = null,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,

    @field:Size(max = 2000, message = "Notes must not exceed 2000 characters")
    val notes: String? = null,
)
