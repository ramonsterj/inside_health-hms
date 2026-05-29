package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryKind
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
            "AND (:kind IS NULL OR i.kind = :kind) " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(i) FROM InventoryItem i " +
            "WHERE (:categoryId IS NULL OR i.category.id = :categoryId) " +
            "AND (:kind IS NULL OR i.kind = :kind) " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
    )
    fun findAllWithFilters(
        categoryId: Long?,
        kind: InventoryKind?,
        search: String,
        pageable: Pageable,
    ): Page<InventoryItem>

    @Query("SELECT i FROM InventoryItem i WHERE i.sku = :sku")
    fun findBySku(sku: String): InventoryItem?

    fun findAllByLotTrackingEnabledTrue(): List<InventoryItem>

    /**
     * Delete all items including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM inventory_items", nativeQuery = true)
    fun deleteAllHard()
}
