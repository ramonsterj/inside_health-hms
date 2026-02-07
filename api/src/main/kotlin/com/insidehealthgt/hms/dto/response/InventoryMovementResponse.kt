package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryMovement
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class InventoryMovementResponse(
    val id: Long,
    val itemId: Long,
    val type: MovementType,
    val quantity: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
) {
    companion object {
        fun from(movement: InventoryMovement, createdByUser: User? = null): InventoryMovementResponse =
            InventoryMovementResponse(
                id = movement.id!!,
                itemId = movement.item.id!!,
                type = movement.movementType,
                quantity = movement.quantity,
                previousQuantity = movement.previousQuantity,
                newQuantity = movement.newQuantity,
                notes = movement.notes,
                createdAt = movement.createdAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            )
    }
}
