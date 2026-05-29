package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WarehouseRepository : JpaRepository<Warehouse, Long> {

    fun findByCode(code: String): Warehouse?

    fun findAllByOrderByName(): List<Warehouse>

    fun existsByCode(code: String): Boolean
}
