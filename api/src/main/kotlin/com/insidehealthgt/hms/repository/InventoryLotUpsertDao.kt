package com.insidehealthgt.hms.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.LocalDate

/**
 * Race-safe ENTRY upsert for inventory_lots.
 *
 * Goes around Spring Data JPA because PostgreSQL `INSERT ... ON CONFLICT ...
 * RETURNING id` against a partial-unique index does not map cleanly onto
 * `@Modifying @Query`. Using JdbcTemplate keeps the SQL explicit and lets us
 * rely on the partial unique index `ux_inventory_lots_item_lot_expiration_active`
 * (created in V102) as the conflict target. That index is defined with
 * `NULLS NOT DISTINCT`, so NULL lot_number rows still conflict deterministically.
 */
@Repository
class InventoryLotUpsertDao(private val jdbc: NamedParameterJdbcTemplate) {

    fun upsertEntry(
        itemId: Long,
        lotNumber: String?,
        expirationDate: LocalDate,
        quantity: Int,
        receivedAt: LocalDate,
        supplier: String?,
    ): Long {
        val params = MapSqlParameterSource()
            .addValue("itemId", itemId)
            .addValue("lotNumber", lotNumber, Types.VARCHAR)
            .addValue("expirationDate", expirationDate)
            .addValue("quantity", quantity)
            .addValue("receivedAt", receivedAt)
            .addValue("supplier", supplier, Types.VARCHAR)

        return jdbc.queryForObject(
            """
            INSERT INTO inventory_lots (
                item_id, lot_number, expiration_date, quantity_on_hand, received_at,
                supplier, recalled, synthetic_legacy, created_at, updated_at
            ) VALUES (
                :itemId, :lotNumber, :expirationDate, :quantity, :receivedAt,
                :supplier, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (item_id, lot_number, expiration_date)
              WHERE deleted_at IS NULL
              DO UPDATE SET
                quantity_on_hand = inventory_lots.quantity_on_hand + EXCLUDED.quantity_on_hand,
                supplier = COALESCE(EXCLUDED.supplier, inventory_lots.supplier),
                updated_at = CURRENT_TIMESTAMP
            RETURNING id
            """.trimIndent(),
            params,
            Long::class.java,
        )!!
    }

    /**
     * Atomic, non-incrementing insert for the explicit "create lot" endpoint.
     *
     * Returns the newly inserted lot id, or `null` if a row with the same
     * (item_id, lot_number, expiration_date) identity already exists (the
     * partial unique index `ux_inventory_lots_item_lot_expiration_active` with
     * `NULLS NOT DISTINCT` is what catches the race). The service layer
     * translates `null` into a 409 conflict — unlike [upsertEntry], we never
     * silently fold the quantities together.
     */
    fun insertIfAbsent(
        itemId: Long,
        lotNumber: String?,
        expirationDate: LocalDate,
        quantity: Int,
        receivedAt: LocalDate,
        supplier: String?,
    ): Long? {
        val params = MapSqlParameterSource()
            .addValue("itemId", itemId)
            .addValue("lotNumber", lotNumber, Types.VARCHAR)
            .addValue("expirationDate", expirationDate)
            .addValue("quantity", quantity)
            .addValue("receivedAt", receivedAt)
            .addValue("supplier", supplier, Types.VARCHAR)

        val ids = jdbc.queryForList(
            """
            INSERT INTO inventory_lots (
                item_id, lot_number, expiration_date, quantity_on_hand, received_at,
                supplier, recalled, synthetic_legacy, created_at, updated_at
            ) VALUES (
                :itemId, :lotNumber, :expirationDate, :quantity, :receivedAt,
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
