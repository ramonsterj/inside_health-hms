package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Warehouse
import java.time.LocalDateTime

data class WarehouseResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    data class Summary(val id: Long, val code: String, val name: String) {
        companion object {
            fun from(warehouse: Warehouse): Summary = Summary(warehouse.id!!, warehouse.code, warehouse.name)
        }
    }

    companion object {
        fun from(warehouse: Warehouse): WarehouseResponse = WarehouseResponse(
            id = warehouse.id!!,
            code = warehouse.code,
            name = warehouse.name,
            description = warehouse.description,
            active = warehouse.active,
            createdAt = warehouse.createdAt,
            updatedAt = warehouse.updatedAt,
        )
    }
}
