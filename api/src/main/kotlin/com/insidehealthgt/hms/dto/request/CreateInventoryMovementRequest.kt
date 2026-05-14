package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.MovementType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateInventoryMovementRequest(
    @field:NotNull(message = "Movement type is required")
    val type: MovementType,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,

    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null,

    val admissionId: Long? = null,

    /** Admin override only — explicit lot for lot-tracked items. Service rejects non-admin senders. */
    val lotId: Long? = null,

    /** ENTRY-only metadata for lot-tracked items; ignored on EXIT and on scalar items. */
    val lotNumber: String? = null,
    val expirationDate: LocalDate? = null,
    val supplier: String? = null,
)
