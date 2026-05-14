package db.migration

import com.insidehealthgt.hms.service.parser.MedicationExpirationParser
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * V111 — One-shot pharmacy catalog seed. Reads the workbook CSV checked into
 * `db/migration/data/pharmacy-initial-load.csv` and loads ~615 items into the
 * pharmacy schema:
 *
 *  - DRUG rows (CSV `kind=DRUG`, sections A/B/C/D) become `inventory_items`
 *    rows under the Medicamentos category plus a `medication_details`
 *    satellite with `review_status='CONFIRMED'`.
 *  - SUPPLY rows (CSV `kind=SUPPLY`, section E) become `inventory_items` rows
 *    under the Material y Equipo category. No satellite.
 *
 * Replaces the deleted `MedicationBulkImportService`. Single source of truth:
 * if the CSV is malformed the migration aborts and Spring fails to start.
 */
@Suppress("ClassNaming", "MagicNumber", "TooManyFunctions")
class V111__seed_pharmacy_from_workbook : BaseJavaMigration() {

    private val expirationParser = MedicationExpirationParser()

    override fun migrate(context: Context) {
        loadInto(context.connection)
    }

    /**
     * Public so integration tests can re-execute the loader against the same
     * schema after the per-test cleanup runs. Production callers go through
     * [migrate] via Flyway.
     */
    fun loadInto(connection: java.sql.Connection) {
        val rows = readCsvRows()
        if (rows.isEmpty()) {
            error("V111: pharmacy-initial-load.csv is empty or unreadable")
        }
        val now = LocalDateTime.now()

        val medicamentosId = resolveCategoryId(connection, "medicament")
        val materialId = resolveCategoryId(connection, "material")

        val itemSql = """
            INSERT INTO inventory_items (
                category_id, name, description, price, cost, quantity, restock_level,
                pricing_type, time_unit, time_interval, active, kind, sku,
                lot_tracking_enabled, created_at, updated_at
            ) VALUES (?, ?, NULL, 0, 0, 0, 0, 'FLAT', NULL, NULL, TRUE, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        val detailsSql = """
            INSERT INTO medication_details (
                item_id, generic_name, commercial_name, strength, dosage_form,
                route, controlled, atc_code, section, review_status, review_notes,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, NULL, FALSE, NULL, ?, 'CONFIRMED', NULL, ?, ?)
        """.trimIndent()

        connection.prepareStatement(itemSql).use { itemStmt ->
            connection.prepareStatement(detailsSql).use { detailsStmt ->
                rows.forEach { row ->
                    insertRow(row, itemStmt, detailsStmt, medicamentosId, materialId, now)
                }
            }
        }
    }

    private fun insertRow(
        row: CsvRow,
        itemStmt: PreparedStatement,
        detailsStmt: PreparedStatement,
        medicamentosId: Long,
        materialId: Long,
        now: LocalDateTime,
    ) {
        val isDrug = row.kind == "DRUG"
        val categoryId = if (isDrug) medicamentosId else materialId

        itemStmt.setLong(1, categoryId)
        itemStmt.setString(2, row.itemName())
        itemStmt.setString(3, row.kind)
        itemStmt.setString(4, row.sku)
        itemStmt.setBoolean(5, isDrug)
        itemStmt.setTimestamp(6, Timestamp.valueOf(now))
        itemStmt.setTimestamp(7, Timestamp.valueOf(now))

        val itemId = itemStmt.executeQuery().use { rs ->
            check(rs.next()) { "V111: INSERT into inventory_items returned no id for ${row.sku}" }
            rs.getLong(1)
        }

        if (isDrug) {
            val section = row.section
                ?: throw IllegalStateException("V111: DRUG row ${row.sku} missing section")
            val dosageForm = inferDosageForm(row, section)
            detailsStmt.setLong(1, itemId)
            detailsStmt.setString(2, row.genericName)
            detailsStmt.setStringOrNull(3, row.commercialName)
            detailsStmt.setStringOrNull(4, row.strength)
            detailsStmt.setString(5, dosageForm)
            detailsStmt.setString(6, section)
            detailsStmt.setTimestamp(7, Timestamp.valueOf(now))
            detailsStmt.setTimestamp(8, Timestamp.valueOf(now))
            detailsStmt.executeUpdate()
        }

        // Note: expiration is captured in the CSV but no lots are seeded.
        // Pharmacist registers real lots via the UI on first restock.
        row.expiration?.let { expirationParser.parseOrNull(it) }
    }

    private fun resolveCategoryId(connection: java.sql.Connection, namePart: String): Long {
        connection.prepareStatement(
            """
            SELECT id FROM inventory_categories
            WHERE LOWER(name) LIKE ? AND deleted_at IS NULL
            ORDER BY id LIMIT 1
            """.trimIndent(),
        ).use { ps ->
            ps.setString(1, "%${namePart.lowercase()}%")
            ps.executeQuery().use { rs ->
                check(rs.next()) { "V111: no inventory_categories row matching '%$namePart%'" }
                return rs.getLong(1)
            }
        }
    }

    private fun inferDosageForm(row: CsvRow, section: String): String {
        row.dosageForm?.takeIf { it.isNotBlank() }?.let { return it.uppercase() }
        return when (section) {
            "AMPOLLA" -> "AMPOULE"
            "JARABE_GOTAS" -> "SYRUP"
            else -> "TABLET"
        }
    }

    private fun readCsvRows(): List<CsvRow> {
        val stream = javaClass.getResourceAsStream("/db/migration/data/pharmacy-initial-load.csv")
            ?: error("V111: pharmacy-initial-load.csv not on classpath")
        val text = stream.bufferedReader(Charsets.UTF_8).readText()
        val lines = parseCsv(text)
        if (lines.size < 2) return emptyList()
        val header = lines.first().map { it.trim().lowercase() }
        return lines.drop(1).mapIndexedNotNull { idx, raw ->
            val sku = header.field(raw, "sku")?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val gen = header.field(raw, "genericname")?.takeIf { it.isNotBlank() }
                ?: error("V111: row ${idx + 2} ($sku) missing genericName")
            val kind = header.field(raw, "kind")?.uppercase()
                ?: error("V111: row ${idx + 2} ($sku) missing kind")
            CsvRow(
                sku = sku,
                genericName = gen,
                commercialName = header.field(raw, "commercialname")?.takeIf { it.isNotBlank() },
                strength = header.field(raw, "strength")?.takeIf { it.isNotBlank() },
                expiration = header.field(raw, "expiration")?.takeIf { it.isNotBlank() },
                section = header.field(raw, "section")?.takeIf { it.isNotBlank() }?.uppercase(),
                dosageForm = header.field(raw, "dosageform"),
                kind = kind,
            )
        }
    }

    private fun List<String>.field(row: List<String>, name: String): String? {
        val idx = indexOf(name).takeIf { it >= 0 } ?: return null
        return row.getOrNull(idx)?.trim()
    }

    private fun parseCsv(text: String): List<List<String>> {
        val state = CsvParseState(text.replace("\r", ""))
        while (state.hasMore()) {
            state.step()
        }
        state.finishTrailingCell()
        return state.rows
    }

    private class CsvParseState(private val source: String) {
        val rows = mutableListOf<List<String>>()
        private val row = mutableListOf<String>()
        private val cell = StringBuilder()
        private var inQuotes = false
        private var i = 0

        fun hasMore(): Boolean = i < source.length

        fun step() {
            val c = source[i]
            if (inQuotes) stepInQuotes(c) else stepOutsideQuotes(c)
        }

        private fun stepInQuotes(c: Char) {
            if (c == '"') {
                if (i + 1 < source.length && source[i + 1] == '"') {
                    cell.append('"')
                    i += 2
                } else {
                    inQuotes = false
                    i++
                }
            } else {
                cell.append(c)
                i++
            }
        }

        private fun stepOutsideQuotes(c: Char) {
            when (c) {
                '"' -> {
                    inQuotes = true
                    i++
                }

                ',' -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                    i++
                }

                '\n' -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row.toList())
                    row.clear()
                    i++
                }

                else -> {
                    cell.append(c)
                    i++
                }
            }
        }

        fun finishTrailingCell() {
            if (cell.isNotEmpty() || row.isNotEmpty()) {
                row.add(cell.toString())
                if (row.any { it.isNotBlank() }) rows.add(row.toList())
            }
        }
    }

    private fun PreparedStatement.setStringOrNull(idx: Int, value: String?) {
        if (value == null) setNull(idx, java.sql.Types.VARCHAR) else setString(idx, value)
    }

    private data class CsvRow(
        val sku: String,
        val genericName: String,
        val commercialName: String?,
        val strength: String?,
        val expiration: String?,
        val section: String?,
        val dosageForm: String?,
        val kind: String,
    ) {
        fun itemName(): String {
            val parts = listOfNotNull(
                genericName,
                commercialName?.takeIf { !genericName.equals(it, ignoreCase = true) },
                strength,
            )
            return parts.joinToString(" ").take(150)
        }
    }
}
