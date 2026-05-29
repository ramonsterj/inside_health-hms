package com.insidehealthgt.hms.pharmacy

import com.insidehealthgt.hms.controller.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import javax.sql.DataSource

/**
 * Guards the dev/acceptance seed against the V120 cut-over: `inventory_items.quantity`
 * was dropped, so `R__seed_02_infrastructure.sql` must no longer write that column and
 * must instead land non-drug starting stock in `inventory_warehouse_stock`
 * (ADMINISTRACION), mirroring the V119 backfill. This reproduces the exact runtime path
 * a fresh dev seed takes: the script runs after all versioned migrations.
 */
class SeedInfrastructureIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    private fun adminStockFor(itemName: String): Int? = jdbcTemplate.query(
        """
        SELECT COALESCE(SUM(s.quantity), 0)
        FROM inventory_warehouse_stock s
        JOIN inventory_items i ON i.id = s.item_id
        JOIN warehouses w ON w.id = s.warehouse_id
        WHERE i.name = ? AND w.code = 'ADMINISTRACION' AND s.deleted_at IS NULL
        """.trimIndent(),
        { rs, _ -> rs.getInt(1) },
        itemName,
    ).firstOrNull()

    private fun stockRowCountFor(itemName: String): Int = jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM inventory_warehouse_stock s
        JOIN inventory_items i ON i.id = s.item_id
        WHERE i.name = ? AND s.deleted_at IS NULL
        """.trimIndent(),
        Int::class.java,
        itemName,
    )!!

    @Test
    fun `R seed infrastructure runs after V120 and lands non-drug stock per warehouse`() {
        // @BeforeEach already cleared inventory_items/stock/lots/movements. The
        // migration-seeded categories (created_by IS NULL) survive that cleanup and
        // R__seed_02 re-inserts them without ON CONFLICT, so drop them first to mirror
        // the truncate that R__seed_01 performs ahead of this script in a real seed run.
        jdbcTemplate.execute("DELETE FROM inventory_categories")

        // Would throw "column \"quantity\" does not exist" before the fix.
        dataSource.connection.use { conn ->
            ScriptUtils.executeSqlScript(conn, ClassPathResource("db/seed/R__seed_02_infrastructure.sql"))
        }

        // Items with quantity > 0 get exactly one ADMINISTRACION stock row at that quantity.
        assertEquals(50, adminStockFor("USO DE GLUCÓMETRO"), "glucometer stock backfilled to ADMINISTRACION")
        assertEquals(97, adminStockFor("KIT DE INGRESO"), "admission kit stock backfilled to ADMINISTRACION")
        assertEquals(100, adminStockFor("ARROZ BLANCO (LIBRA)"), "kitchen stock backfilled to ADMINISTRACION")

        // Items with quantity 0 (services / equipment) get no stock row at all.
        assertEquals(0, stockRowCountFor("EKG"), "zero-quantity items must not create a stock row")
        assertEquals(0, stockRowCountFor("ATENCIÓN EMERGENCIA"), "zero-quantity services must not create a stock row")
    }
}
