package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryLot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Lots are now metadata only (lot number, expiration, supplier, recall flag).
 * Their on-hand quantity lives per-warehouse in `inventory_warehouse_stock`; the
 * warehouse-scoped FEFO write path is [InventoryWarehouseStockRepository.findFefoForUpdate].
 */
@Repository
@Suppress("TooManyFunctions") // Repository naturally has many fine-grained query methods.
interface InventoryLotRepository : JpaRepository<InventoryLot, Long> {

    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId " +
            "AND (:lotNumber IS NULL AND l.lotNumber IS NULL OR l.lotNumber = :lotNumber) " +
            "AND l.expirationDate = :expirationDate",
    )
    fun findByItemIdAndLotNumberAndExpirationDate(
        @Param("itemId") itemId: Long,
        @Param("lotNumber") lotNumber: String?,
        @Param("expirationDate") expirationDate: LocalDate,
    ): InventoryLot?

    @Query(
        "SELECT l FROM InventoryLot l JOIN FETCH l.item " +
            "WHERE l.item.id = :itemId ORDER BY l.expirationDate ASC",
    )
    fun findAllByItemIdOrderByExpirationDate(@Param("itemId") itemId: Long): List<InventoryLot>

    @Query(
        value = "SELECT EXISTS(" +
            "SELECT 1 FROM inventory_movements " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsMovementsByLotId(@Param("lotId") lotId: Long): Boolean

    @Query(
        value = "SELECT EXISTS(" +
            "SELECT 1 FROM medication_administrations " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsAdministrationsByLotId(@Param("lotId") lotId: Long): Boolean

    /** True if any warehouse holds stock against this lot — blocks lot delete. */
    @Query(
        value = "SELECT EXISTS(" +
            "SELECT 1 FROM inventory_warehouse_stock " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsWarehouseStockByLotId(@Param("lotId") lotId: Long): Boolean

    fun countByItemIdAndDeletedAtIsNull(itemId: Long): Long

    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId AND l.syntheticLegacy = true",
    )
    fun findSyntheticLegacyForItem(@Param("itemId") itemId: Long): List<InventoryLot>

    @Query(
        "SELECT l FROM InventoryLot l JOIN FETCH l.item i LEFT JOIN FETCH i.category " +
            "WHERE l.recalled = false " +
            "ORDER BY l.expirationDate ASC",
    )
    fun findAllActiveWithItem(): List<InventoryLot>

    /**
     * Soonest-to-expire active lot per item that still has positive stock in any
     * warehouse, for a batch of items. Used by the Kardex aggregator to render an
     * expiry chip next to each medication without an N+1 fefo call.
     */
    @Query(
        value = "SELECT s.item_id AS itemId, MIN(l.expiration_date) AS expirationDate " +
            "FROM inventory_warehouse_stock s " +
            "JOIN inventory_lots l ON l.id = s.lot_id " +
            "WHERE s.item_id IN (:itemIds) " +
            "  AND s.deleted_at IS NULL " +
            "  AND l.deleted_at IS NULL " +
            "  AND l.recalled = FALSE " +
            "  AND s.quantity > 0 " +
            "GROUP BY s.item_id",
        nativeQuery = true,
    )
    fun findNextExpirationByItemIds(@Param("itemIds") itemIds: List<Long>): List<NextExpirationRow>

    interface NextExpirationRow {
        val itemId: Long
        val expirationDate: LocalDate
    }
}
