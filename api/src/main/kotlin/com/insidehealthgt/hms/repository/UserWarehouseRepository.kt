package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.UserWarehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserWarehouseRepository : JpaRepository<UserWarehouse, Long> {

    @Query("SELECT uw FROM UserWarehouse uw JOIN FETCH uw.warehouse WHERE uw.user.id = :userId")
    fun findByUserId(@Param("userId") userId: Long): List<UserWarehouse>

    fun existsByUserIdAndWarehouseId(userId: Long, warehouseId: Long): Boolean
}
