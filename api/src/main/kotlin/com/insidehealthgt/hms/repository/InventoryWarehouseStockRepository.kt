package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryWarehouseStock
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Per-warehouse stock. All stock mutations serialize here: the [findForUpdate]
 * and [findFefoForUpdate] queries take a PESSIMISTIC_WRITE lock so concurrent
 * dispenses / transfers / charges on the same row cannot oversell (NFR
 * concurrency, AC-16).
 */
@Repository
@Suppress("TooManyFunctions")
interface InventoryWarehouseStockRepository : JpaRepository<InventoryWarehouseStock, Long> {

    /**
     * Lock the stock row for (item, warehouse, lot) — lot may be null for
     * non-lot-tracked items. The serialization point for every scalar mutation
     * and for lot-override EXIT.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT s FROM InventoryWarehouseStock s " +
            "WHERE s.item.id = :itemId AND s.warehouse.id = :warehouseId " +
            "AND ((:lotId IS NULL AND s.lot IS NULL) OR s.lot.id = :lotId)",
    )
    fun findForUpdate(
        @Param("itemId") itemId: Long,
        @Param("warehouseId") warehouseId: Long,
        @Param("lotId") lotId: Long?,
    ): InventoryWarehouseStock?

    /**
     * Warehouse-scoped FEFO selection (write path): pessimistic-lock the
     * soonest-to-expire non-recalled lot stock row in the given warehouse with
     * enough on-hand to cover [minQty]. Mirrors InventoryLotRepository's FEFO but
     * filtered to one warehouse (AC-5).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT s FROM InventoryWarehouseStock s JOIN s.lot l " +
            "WHERE s.item.id = :itemId AND s.warehouse.id = :warehouseId " +
            "AND l.recalled = false AND s.quantity >= :minQty " +
            "ORDER BY l.expirationDate ASC",
    )
    fun findFefoForUpdate(
        @Param("itemId") itemId: Long,
        @Param("warehouseId") warehouseId: Long,
        @Param("minQty") minQty: BigDecimal,
    ): List<InventoryWarehouseStock>

    /** System-wide on-hand for an item, summed across all warehouses and lots. */
    @Query(
        value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory_warehouse_stock " +
            "WHERE item_id = :itemId AND deleted_at IS NULL",
        nativeQuery = true,
    )
    fun sumByItem(@Param("itemId") itemId: Long): BigDecimal

    /** On-hand for an item within a single warehouse. */
    @Query(
        value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory_warehouse_stock " +
            "WHERE item_id = :itemId AND warehouse_id = :warehouseId AND deleted_at IS NULL",
        nativeQuery = true,
    )
    fun sumByItemAndWarehouse(@Param("itemId") itemId: Long, @Param("warehouseId") warehouseId: Long): BigDecimal

    /** System-wide on-hand for a single lot (summed across warehouses). */
    @Query(
        value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory_warehouse_stock " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL",
        nativeQuery = true,
    )
    fun sumByLot(@Param("lotId") lotId: Long): BigDecimal

    /** Batch system-wide sums for a set of lots (avoids N+1 on the lot list). */
    @Query(
        value = "SELECT lot_id AS lotId, COALESCE(SUM(quantity), 0) AS quantity " +
            "FROM inventory_warehouse_stock WHERE lot_id IN (:lotIds) AND deleted_at IS NULL " +
            "GROUP BY lot_id",
        nativeQuery = true,
    )
    fun sumByLotIds(@Param("lotIds") lotIds: List<Long>): List<LotQuantityRow>

    /**
     * Non-locking warehouse-scoped FEFO peek for the UI preview (no row lock, so
     * concurrent previews never block a dispense). Mirrors [findFefoForUpdate].
     */
    @Query(
        "SELECT s FROM InventoryWarehouseStock s JOIN s.lot l " +
            "WHERE s.item.id = :itemId AND s.warehouse.id = :warehouseId " +
            "AND l.recalled = false AND s.quantity >= :minQty " +
            "ORDER BY l.expirationDate ASC",
    )
    fun peekFefoForWarehouse(
        @Param("itemId") itemId: Long,
        @Param("warehouseId") warehouseId: Long,
        @Param("minQty") minQty: BigDecimal,
    ): List<InventoryWarehouseStock>

    /** Batch system-wide sums for a page of items (avoids N+1 on the catalog list). */
    @Query(
        value = "SELECT item_id AS itemId, COALESCE(SUM(quantity), 0) AS quantity " +
            "FROM inventory_warehouse_stock WHERE item_id IN (:itemIds) AND deleted_at IS NULL " +
            "GROUP BY item_id",
        nativeQuery = true,
    )
    fun sumByItemIds(@Param("itemIds") itemIds: List<Long>): List<ItemQuantityRow>

    /** Batch per-warehouse sums for a page of items. */
    @Query(
        value = "SELECT item_id AS itemId, COALESCE(SUM(quantity), 0) AS quantity " +
            "FROM inventory_warehouse_stock " +
            "WHERE item_id IN (:itemIds) AND warehouse_id = :warehouseId AND deleted_at IS NULL " +
            "GROUP BY item_id",
        nativeQuery = true,
    )
    fun sumByItemIdsAndWarehouse(
        @Param("itemIds") itemIds: List<Long>,
        @Param("warehouseId") warehouseId: Long,
    ): List<ItemQuantityRow>

    /** True if the warehouse holds any positive stock — gate for warehouse delete (AC-11). */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM inventory_warehouse_stock " +
            "WHERE warehouse_id = :warehouseId AND quantity > 0 AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsPositiveStockInWarehouse(@Param("warehouseId") warehouseId: Long): Boolean

    /** Per-lot on-hand within a warehouse, for lots with positive stock (expiry-report facet, AC-15). */
    @Query(
        value = "SELECT lot_id AS lotId, COALESCE(SUM(quantity), 0) AS quantity FROM inventory_warehouse_stock " +
            "WHERE warehouse_id = :warehouseId AND lot_id IS NOT NULL AND deleted_at IS NULL " +
            "GROUP BY lot_id HAVING SUM(quantity) > 0",
        nativeQuery = true,
    )
    fun findLotStockInWarehouse(@Param("warehouseId") warehouseId: Long): List<LotQuantityRow>

    /**
     * Per-warehouse stock view, aggregated to one row per item (lot-tracked items
     * collapse their lot rows into a single on-hand sum). Optional name/sku search
     * and low-stock filter (FR-6, FR-7).
     */
    @Query(
        value = "SELECT i.id AS itemId, i.name AS name, i.sku AS sku, i.kind AS kind, " +
            "i.price AS price, i.restock_level AS restockLevel, " +
            "COALESCE(SUM(s.quantity), 0) AS quantity " +
            "FROM inventory_warehouse_stock s " +
            "JOIN inventory_items i ON i.id = s.item_id " +
            "WHERE s.warehouse_id = :warehouseId AND s.deleted_at IS NULL AND i.deleted_at IS NULL " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY i.id, i.name, i.sku, i.kind, i.price, i.restock_level " +
            "HAVING (:lowStockOnly = FALSE OR (i.restock_level > 0 AND SUM(s.quantity) <= i.restock_level)) " +
            "ORDER BY i.name ASC",
        countQuery = "SELECT COUNT(*) FROM (" +
            "SELECT i.id FROM inventory_warehouse_stock s " +
            "JOIN inventory_items i ON i.id = s.item_id " +
            "WHERE s.warehouse_id = :warehouseId AND s.deleted_at IS NULL AND i.deleted_at IS NULL " +
            "AND (:search = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY i.id, i.restock_level " +
            "HAVING (:lowStockOnly = FALSE OR (i.restock_level > 0 AND SUM(s.quantity) <= i.restock_level))" +
            ") sub",
        nativeQuery = true,
    )
    fun findStockForWarehouse(
        @Param("warehouseId") warehouseId: Long,
        @Param("search") search: String,
        @Param("lowStockOnly") lowStockOnly: Boolean,
        pageable: Pageable,
    ): Page<WarehouseStockRow>

    /**
     * System-wide low stock: items whose total on-hand across every warehouse is
     * at or below their restock level. LEFT JOIN so zero-stock items still appear
     * (preserves the legacy behavior where quantity defaulted to 0).
     */
    @Query(
        value = "SELECT i.id AS itemId, COALESCE(SUM(s.quantity), 0) AS quantity " +
            "FROM inventory_items i " +
            "LEFT JOIN inventory_warehouse_stock s ON s.item_id = i.id AND s.deleted_at IS NULL " +
            "WHERE i.deleted_at IS NULL AND i.restock_level > 0 " +
            "AND (:categoryId IS NULL OR i.category_id = :categoryId) " +
            "GROUP BY i.id, i.restock_level " +
            "HAVING COALESCE(SUM(s.quantity), 0) <= i.restock_level " +
            "ORDER BY (i.restock_level - COALESCE(SUM(s.quantity), 0)) DESC",
        nativeQuery = true,
    )
    fun findSystemWideLowStock(@Param("categoryId") categoryId: Long?): List<ItemQuantityRow>

    /**
     * Per-warehouse low stock: items the warehouse stocks whose on-hand there is
     * at or below their restock level (FR-7, AC-14). INNER JOIN — an item the
     * warehouse does not carry is not "low" for that warehouse.
     */
    @Query(
        value = "SELECT i.id AS itemId, COALESCE(SUM(s.quantity), 0) AS quantity " +
            "FROM inventory_items i " +
            "JOIN inventory_warehouse_stock s ON s.item_id = i.id AND s.deleted_at IS NULL " +
            "WHERE i.deleted_at IS NULL AND i.restock_level > 0 AND s.warehouse_id = :warehouseId " +
            "AND (:categoryId IS NULL OR i.category_id = :categoryId) " +
            "GROUP BY i.id, i.restock_level " +
            "HAVING COALESCE(SUM(s.quantity), 0) <= i.restock_level " +
            "ORDER BY (i.restock_level - COALESCE(SUM(s.quantity), 0)) DESC",
        nativeQuery = true,
    )
    fun findWarehouseLowStock(
        @Param("warehouseId") warehouseId: Long,
        @Param("categoryId") categoryId: Long?,
    ): List<ItemQuantityRow>

    /**
     * Per-warehouse on-hand breakdown for a single item across **all active**
     * warehouses — including warehouses with 0 on-hand (LEFT JOIN), so a viewer
     * always sees their dispensing bodega listed even when it is empty (FR-11).
     * Drives the pharmacy detail stock-transparency panel.
     */
    @Query(
        value = "SELECT w.id AS warehouseId, w.code AS code, w.name AS name, " +
            "COALESCE(SUM(s.quantity), 0) AS quantity " +
            "FROM warehouses w " +
            "LEFT JOIN inventory_warehouse_stock s " +
            "  ON s.warehouse_id = w.id AND s.item_id = :itemId AND s.deleted_at IS NULL " +
            "WHERE w.deleted_at IS NULL AND w.active = TRUE " +
            "GROUP BY w.id, w.code, w.name " +
            "ORDER BY w.name ASC",
        nativeQuery = true,
    )
    fun findWarehouseBreakdownForItem(@Param("itemId") itemId: Long): List<WarehouseBreakdownRow>

    /** Positive stock still sitting on recalled lots — flagged by the daily integrity job. */
    @Query(
        value =
        "SELECT s.item_id AS itemId, s.lot_id AS lotId, s.warehouse_id AS warehouseId, " +
            "s.quantity AS quantity " +
            "FROM inventory_warehouse_stock s " +
            "JOIN inventory_lots l ON l.id = s.lot_id " +
            "WHERE s.deleted_at IS NULL AND l.recalled = TRUE AND s.quantity > 0",
        nativeQuery = true,
    )
    fun findRecalledLotStock(): List<RecalledStockRow>

    interface ItemQuantityRow {
        val itemId: Long
        val quantity: BigDecimal
    }

    interface LotQuantityRow {
        val lotId: Long
        val quantity: BigDecimal
    }

    interface RecalledStockRow {
        val itemId: Long
        val lotId: Long
        val warehouseId: Long
        val quantity: BigDecimal
    }

    interface WarehouseBreakdownRow {
        val warehouseId: Long
        val code: String
        val name: String
        val quantity: BigDecimal
    }

    interface WarehouseStockRow {
        val itemId: Long
        val name: String
        val sku: String?
        val kind: String
        val price: BigDecimal
        val restockLevel: Int
        val quantity: BigDecimal
    }
}
