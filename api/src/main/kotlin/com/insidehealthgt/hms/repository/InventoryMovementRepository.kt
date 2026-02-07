package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface InventoryMovementRepository : JpaRepository<InventoryMovement, Long> {

    @Query(
        "SELECT m FROM InventoryMovement m " +
            "WHERE m.item.id = :itemId " +
            "ORDER BY m.createdAt DESC",
    )
    fun findByItemIdOrderByCreatedAtDesc(itemId: Long): List<InventoryMovement>

    /**
     * Delete all movements including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM inventory_movements", nativeQuery = true)
    fun deleteAllHard()
}
