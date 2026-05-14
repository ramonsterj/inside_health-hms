package db.migration

import com.insidehealthgt.hms.service.parser.MedicationNameParser
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * V105 — Backfill MedicationDetails for every legacy `kind=DRUG` inventory item
 * by parsing the free-text `name` column (and looking up category names) with
 * [MedicationNameParser]. High-confidence parses are saved with CONFIRMED;
 * low-confidence are NEEDS_REVIEW with notes explaining why.
 *
 * Idempotent: skips items that already have a `medication_details` row.
 */
@Suppress("ClassNaming", "MagicNumber")
class V105__backfill_medication_details_from_v052_items : BaseJavaMigration() {

    private val parser = MedicationNameParser()

    override fun migrate(context: Context) {
        val connection = context.connection
        val now = LocalDateTime.now()

        val drugItems = mutableListOf<Triple<Long, String, String?>>() // id, name, categoryName
        connection.createStatement().use { st ->
            st.executeQuery(
                """
                SELECT i.id, i.name, c.name AS category_name
                FROM inventory_items i
                LEFT JOIN inventory_categories c ON c.id = i.category_id
                WHERE i.kind = 'DRUG'
                  AND i.deleted_at IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM medication_details md
                    WHERE md.item_id = i.id AND md.deleted_at IS NULL
                  )
                """.trimIndent(),
            ).use { rs ->
                while (rs.next()) {
                    drugItems += Triple(rs.getLong(1), rs.getString(2), rs.getString(3))
                }
            }
        }

        if (drugItems.isEmpty()) return

        val insertSql = """
            INSERT INTO medication_details (
                item_id, generic_name, commercial_name, strength, dosage_form,
                route, controlled, atc_code, section, review_status, review_notes,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(insertSql).use { ps ->
            drugItems.forEach { (id, name, categoryName) ->
                val result = parser.parse(name, categoryName)
                ps.setLong(1, id)
                ps.setString(2, result.genericName)
                ps.setStringOrNull(3, result.commercialName)
                ps.setStringOrNull(4, result.strength)
                ps.setString(5, result.dosageForm.name)
                ps.setStringOrNull(6, result.route?.name)
                ps.setBoolean(7, false)
                ps.setStringOrNull(8, null)
                ps.setString(9, result.section.name)
                ps.setString(10, result.reviewStatus.name)
                ps.setStringOrNull(11, result.reviewNotes)
                ps.setTimestamp(12, Timestamp.valueOf(now))
                ps.setTimestamp(13, Timestamp.valueOf(now))
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun PreparedStatement.setStringOrNull(idx: Int, value: String?) {
        if (value == null) this.setNull(idx, java.sql.Types.VARCHAR) else this.setString(idx, value)
    }
}
