package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface InventoryItemRepository : JpaRepository<InventoryItem, Long> {

    @Query(
        value = "SELECT i FROM InventoryItem i JOIN FETCH i.category " +
            "WHERE (:categoryId IS NULL OR i.category.id = :categoryId) " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(i) FROM InventoryItem i " +
            "WHERE (:categoryId IS NULL OR i.category.id = :categoryId) " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))",
    )
    fun findAllWithFilters(categoryId: Long?, search: String, pageable: Pageable): Page<InventoryItem>

    @Query(
        "SELECT i FROM InventoryItem i JOIN FETCH i.category " +
            "WHERE i.quantity <= i.restockLevel AND i.restockLevel > 0 " +
            "AND (:categoryId IS NULL OR i.category.id = :categoryId) " +
            "ORDER BY (i.restockLevel - i.quantity) DESC",
    )
    fun findLowStock(categoryId: Long?): List<InventoryItem>

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE inventory_items SET quantity = quantity + :delta, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id AND quantity + :delta >= 0",
        nativeQuery = true,
    )
    fun updateQuantityAtomically(id: Long, delta: Int): Int

    @Query(value = "SELECT quantity FROM inventory_items WHERE id = :id", nativeQuery = true)
    fun findCurrentQuantity(id: Long): Int

    /**
     * Delete all items including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM inventory_items", nativeQuery = true)
    fun deleteAllHard()
}
