package com.insidehealthgt.hms.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Race-safe additive upsert for inventory_warehouse_stock — the destination side
 * of a transfer or an ENTRY movement.
 *
 * Mirrors [InventoryLotUpsertDao]: PostgreSQL `INSERT ... ON CONFLICT ...
 * RETURNING id` against the partial-unique index `uq_iws_item_wh_lot` (created in
 * V119) does not map cleanly onto `@Modifying @Query`. The conflict target uses
 * `COALESCE(lot_id, -1)` so NULL-lot (non-lot-tracked) rows still collide
 * deterministically.
 */
@Repository
class InventoryWarehouseStockUpsertDao(private val jdbc: NamedParameterJdbcTemplate) {

    /** Add [quantity] to the (item, warehouse, lot) stock row, creating it if absent. Returns the row id. */
    fun upsertAdd(itemId: Long, warehouseId: Long, lotId: Long?, quantity: BigDecimal): Long {
        val params = MapSqlParameterSource()
            .addValue("itemId", itemId)
            .addValue("warehouseId", warehouseId)
            .addValue("lotId", lotId)
            .addValue("quantity", quantity)

        return jdbc.queryForObject(
            """
            INSERT INTO inventory_warehouse_stock (
                item_id, warehouse_id, lot_id, quantity, created_at, updated_at
            ) VALUES (
                :itemId, :warehouseId, :lotId, :quantity, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (item_id, warehouse_id, COALESCE(lot_id, -1))
              WHERE deleted_at IS NULL
              DO UPDATE SET
                quantity = inventory_warehouse_stock.quantity + EXCLUDED.quantity,
                updated_at = CURRENT_TIMESTAMP
            RETURNING id
            """.trimIndent(),
            params,
            Long::class.java,
        )!!
    }
}
