package com.insidehealthgt.hms.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.LocalDate

/**
 * Race-safe lot-identity upsert for inventory_lots.
 *
 * Lots are metadata only since the warehouse cutover (V121 dropped
 * `quantity_on_hand`); on-hand quantity lives in `inventory_warehouse_stock`.
 * These methods only ensure the (item, lotNumber, expirationDate) identity row
 * exists and return its id — the caller then adds quantity to the warehouse
 * stock via [InventoryWarehouseStockUpsertDao].
 *
 * Goes around Spring Data JPA because PostgreSQL `INSERT ... ON CONFLICT ...
 * RETURNING id` against the partial-unique index
 * `ux_inventory_lots_item_lot_expiration_active` (V102, `NULLS NOT DISTINCT`)
 * does not map cleanly onto `@Modifying @Query`.
 */
@Repository
class InventoryLotUpsertDao(private val jdbc: NamedParameterJdbcTemplate) {

    /**
     * Find-or-create the lot identity for an ENTRY movement. Returns the lot id
     * whether it already existed or was just created (supplier refreshed on
     * conflict). Quantity is added to warehouse stock separately.
     */
    fun upsertEntry(
        itemId: Long,
        lotNumber: String?,
        expirationDate: LocalDate,
        receivedAt: LocalDate,
        supplier: String?,
    ): Long {
        val params = MapSqlParameterSource()
            .addValue("itemId", itemId)
            .addValue("lotNumber", lotNumber, Types.VARCHAR)
            .addValue("expirationDate", expirationDate)
            .addValue("receivedAt", receivedAt)
            .addValue("supplier", supplier, Types.VARCHAR)

        return jdbc.queryForObject(
            """
            INSERT INTO inventory_lots (
                item_id, lot_number, expiration_date, received_at,
                supplier, recalled, synthetic_legacy, created_at, updated_at
            ) VALUES (
                :itemId, :lotNumber, :expirationDate, :receivedAt,
                :supplier, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (item_id, lot_number, expiration_date)
              WHERE deleted_at IS NULL
              DO UPDATE SET
                supplier = COALESCE(EXCLUDED.supplier, inventory_lots.supplier),
                updated_at = CURRENT_TIMESTAMP
            RETURNING id
            """.trimIndent(),
            params,
            Long::class.java,
        )!!
    }

    /**
     * Atomic insert for the explicit "create lot" endpoint. Returns the new lot
     * id, or `null` if a row with the same (item_id, lot_number, expiration_date)
     * identity already exists (the service translates `null` into a 409).
     */
    fun insertIfAbsent(
        itemId: Long,
        lotNumber: String?,
        expirationDate: LocalDate,
        receivedAt: LocalDate,
        supplier: String?,
    ): Long? {
        val params = MapSqlParameterSource()
            .addValue("itemId", itemId)
            .addValue("lotNumber", lotNumber, Types.VARCHAR)
            .addValue("expirationDate", expirationDate)
            .addValue("receivedAt", receivedAt)
            .addValue("supplier", supplier, Types.VARCHAR)

        val ids = jdbc.queryForList(
            """
            INSERT INTO inventory_lots (
                item_id, lot_number, expiration_date, received_at,
                supplier, recalled, synthetic_legacy, created_at, updated_at
            ) VALUES (
                :itemId, :lotNumber, :expirationDate, :receivedAt,
                :supplier, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (item_id, lot_number, expiration_date)
              WHERE deleted_at IS NULL
              DO NOTHING
            RETURNING id
            """.trimIndent(),
            params,
            Long::class.java,
        )
        return ids.firstOrNull()
    }
}
