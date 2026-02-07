package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface InventoryCategoryRepository : JpaRepository<InventoryCategory, Long> {

    fun findAllByOrderByDisplayOrderAsc(): List<InventoryCategory>

    fun findAllByActiveTrueOrderByDisplayOrderAsc(): List<InventoryCategory>

    fun existsByName(name: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM InventoryCategory c WHERE c.name = :name AND c.id != :excludeId",
    )
    fun existsByNameExcludingId(name: String, excludeId: Long): Boolean

    /**
     * Check if a category has inventory items (including soft-deleted ones).
     * Uses native query to bypass the @SQLRestriction soft delete filter.
     */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM inventory_items WHERE category_id = :categoryId)",
        nativeQuery = true,
    )
    fun existsItemsByCategoryIdIncludingDeleted(categoryId: Long): Boolean

    /**
     * Delete all categories including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM inventory_categories", nativeQuery = true)
    fun deleteAllHard()
}
