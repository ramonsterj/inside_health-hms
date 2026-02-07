package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryCategory
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class InventoryCategoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val displayOrder: Int,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(category: InventoryCategory, createdByUser: User?, updatedByUser: User?): InventoryCategoryResponse =
            InventoryCategoryResponse(
                id = category.id!!,
                name = category.name,
                description = category.description,
                displayOrder = category.displayOrder,
                active = category.active,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
                updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
            )

        fun fromSimple(category: InventoryCategory): InventoryCategoryResponse = InventoryCategoryResponse(
            id = category.id!!,
            name = category.name,
            description = category.description,
            displayOrder = category.displayOrder,
            active = category.active,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt,
            createdBy = null,
            updatedBy = null,
        )
    }
}
