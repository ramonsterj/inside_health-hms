package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryTransfer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InventoryTransferRepository : JpaRepository<InventoryTransfer, Long> {

    @Query(
        value = "SELECT t FROM InventoryTransfer t " +
            "JOIN FETCH t.sourceWarehouse JOIN FETCH t.destinationWarehouse JOIN FETCH t.item " +
            "WHERE (:warehouseId IS NULL OR t.sourceWarehouse.id = :warehouseId " +
            "       OR t.destinationWarehouse.id = :warehouseId) " +
            "AND (:itemId IS NULL OR t.item.id = :itemId)",
        countQuery = "SELECT COUNT(t) FROM InventoryTransfer t " +
            "WHERE (:warehouseId IS NULL OR t.sourceWarehouse.id = :warehouseId " +
            "       OR t.destinationWarehouse.id = :warehouseId) " +
            "AND (:itemId IS NULL OR t.item.id = :itemId)",
    )
    fun findHistory(
        @Param("warehouseId") warehouseId: Long?,
        @Param("itemId") itemId: Long?,
        pageable: Pageable,
    ): Page<InventoryTransfer>
}
