package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.entity.TimeUnit
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDateTime

data class InventoryItemResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val category: CategorySummary,
    val price: BigDecimal,
    val cost: BigDecimal,
    val quantity: Int,
    val restockLevel: Int,
    val pricingType: PricingType,
    val timeUnit: TimeUnit?,
    val timeInterval: Int?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    data class CategorySummary(val id: Long, val name: String)

    companion object {
        fun from(
            item: InventoryItem,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ): InventoryItemResponse = InventoryItemResponse(
            id = item.id!!,
            name = item.name,
            description = item.description,
            category = CategorySummary(
                id = item.category.id!!,
                name = item.category.name,
            ),
            price = item.price,
            cost = item.cost,
            quantity = item.quantity,
            restockLevel = item.restockLevel,
            pricingType = item.pricingType,
            timeUnit = item.timeUnit,
            timeInterval = item.timeInterval,
            active = item.active,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )
    }
}
