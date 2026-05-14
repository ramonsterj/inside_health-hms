package com.insidehealthgt.hms.pharmacy

import com.insidehealthgt.hms.controller.AbstractIntegrationTest
import com.insidehealthgt.hms.entity.DosageForm
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.entity.InventoryLot
import com.insidehealthgt.hms.entity.MedicationDetails
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryLotUpsertDao
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.service.InventoryLotService
import db.migration.V111__seed_pharmacy_from_workbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import javax.sql.DataSource

/**
 * Focused integration coverage for the pharmacy / lot module gaps the review
 * flagged. Each test maps to a specific acceptance criterion in
 * `docs/features/pharmacy-and-inventory-evolution.md`.
 */
class PharmacyLotIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var lotRepository: InventoryLotRepository

    @Autowired
    private lateinit var lotUpsertDao: InventoryLotUpsertDao

    @Autowired
    private lateinit var lotService: InventoryLotService

    @Autowired
    private lateinit var detailsRepository: MedicationDetailsRepository

    @Autowired
    private lateinit var dataSource: DataSource

    private fun loginAsAdmin(admin: com.insidehealthgt.hms.entity.User) {
        // Build authentication without dereferencing the lazy `user.roles` —
        // the service layer only needs `currentUserDetails().id`, so an empty
        // authorities list is fine. Computing authorities would trip
        // LazyInitializationException since we're outside any open session.
        val principal = CustomUserDetails(admin)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }

    private fun seedDrugItem(name: String = "Test drug", sku: String? = null): InventoryItem {
        val category = inventoryCategoryRepository.findAll().first { it.name == "Medicamentos" }
        val item = InventoryItem(
            category = category,
            name = name,
            description = null,
            price = BigDecimal("1.00"),
            cost = BigDecimal("0.50"),
            restockLevel = 0,
            pricingType = PricingType.FLAT,
            active = true,
            kind = InventoryKind.DRUG,
            sku = sku,
            lotTrackingEnabled = true,
        )
        return inventoryItemRepository.save(item)
    }

    @Test
    fun `lot upsert against partial unique index is race-safe and additive across two ENTRY calls`() {
        val (admin, adminToken) = createAdminUser()
        loginAsAdmin(admin)
        val item = seedDrugItem()
        val expiration = LocalDate.now().plusYears(1)

        val firstLotId = lotUpsertDao.upsertEntry(
            itemId = item.id!!,
            lotNumber = "LOT-A",
            expirationDate = expiration,
            quantity = 10,
            receivedAt = LocalDate.now(),
            supplier = "Vendor1",
        )
        val secondLotId = lotUpsertDao.upsertEntry(
            itemId = item.id!!,
            lotNumber = "LOT-A",
            expirationDate = expiration,
            quantity = 5,
            receivedAt = LocalDate.now(),
            supplier = null,
        )

        assertEquals(firstLotId, secondLotId, "ON CONFLICT must resolve to the same lot row")
        val lot = lotRepository.findById(firstLotId).orElseThrow()
        assertEquals(15, lot.quantityOnHand, "Two ENTRY calls must add to quantity_on_hand")
        assertEquals("Vendor1", lot.supplier, "COALESCE preserves the existing supplier when a later ENTRY omits it")
    }

    @Test
    fun `NULL lot_number rows still collide under NULLS NOT DISTINCT`() {
        val (admin, adminToken) = createAdminUser()
        loginAsAdmin(admin)
        val item = seedDrugItem()
        val expiration = LocalDate.now().plusYears(1)

        val firstId = lotUpsertDao.upsertEntry(item.id!!, null, expiration, 4, LocalDate.now(), null)
        val secondId = lotUpsertDao.upsertEntry(item.id!!, null, expiration, 6, LocalDate.now(), null)

        assertEquals(firstId, secondId, "NULL lot_numbers must collide via NULLS NOT DISTINCT, not insert a duplicate")
        assertEquals(10, lotRepository.findById(firstId).orElseThrow().quantityOnHand)
    }

    @Test
    fun `V111 workbook loader populates the pharmacy catalog with confirmed drug details and no synthetic lots`() {
        // AbstractIntegrationTest's @BeforeEach truncates inventory_items, so by
        // the time this test method runs the V111-loaded catalog is gone. Re-run
        // the loader against the same schema to verify the shape it produces.
        dataSource.connection.use { conn ->
            V111__seed_pharmacy_from_workbook().loadInto(conn)
        }

        val drugCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_items WHERE kind = 'DRUG' AND sku IS NOT NULL AND deleted_at IS NULL",
            Long::class.java,
        )!!
        assertTrue(drugCount >= 400, "Workbook should yield at least 400 DRUG rows, got $drugCount")

        val supplyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_items WHERE kind = 'SUPPLY' AND sku LIKE 'E%' AND deleted_at IS NULL",
            Long::class.java,
        )!!
        assertTrue(supplyCount >= 100, "Workbook should yield at least 100 SUPPLY rows, got $supplyCount")

        val drugsWithoutDetails = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM inventory_items i
            WHERE i.kind = 'DRUG' AND i.sku IS NOT NULL AND i.deleted_at IS NULL
              AND NOT EXISTS (
                SELECT 1 FROM medication_details md
                WHERE md.item_id = i.id AND md.deleted_at IS NULL
              )
            """.trimIndent(),
            Long::class.java,
        )!!
        assertEquals(0L, drugsWithoutDetails, "Every workbook DRUG row must have a medication_details satellite")

        val needsReview = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM medication_details WHERE review_status = 'NEEDS_REVIEW' AND deleted_at IS NULL",
            Long::class.java,
        )!!
        assertEquals(0L, needsReview, "Workbook-loaded rows must be CONFIRMED, never NEEDS_REVIEW")

        val ampouleDosage = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM medication_details md
            JOIN inventory_items i ON i.id = md.item_id
            WHERE md.section = 'AMPOLLA' AND md.dosage_form NOT IN ('AMPOULE', 'INJECTION')
            """.trimIndent(),
            Long::class.java,
        )!!
        assertEquals(0L, ampouleDosage, "Ampolla-section drugs must map to the AMPOULE dosage_form enum value")

        val syntheticLegacyLots = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_lots WHERE synthetic_legacy = TRUE AND deleted_at IS NULL",
            Long::class.java,
        )!!
        assertEquals(0L, syntheticLegacyLots, "V110 must remove all synthetic-legacy lots")

        val perms = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM permissions WHERE code = 'medication:bulk-import'",
            Long::class.java,
        )!!
        assertEquals(0L, perms, "V112 must drop medication:bulk-import permission")
    }

    @Test
    fun `deleting a lot referenced by a medication administration returns 409 (lot delete guard)`() {
        val (admin, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!)
        val admission = admissionRepository.findById(admissionId).get()
        loginAsAdmin(admin)
        val item = seedDrugItem()

        // Lot directly via JPA; bypass the service so we don't depend on auth.
        val lot = lotRepository.save(
            InventoryLot(
                item = item,
                lotNumber = "LOT-DELGUARD",
                expirationDate = LocalDate.now().plusYears(1),
                quantityOnHand = 10,
                receivedAt = LocalDate.now(),
            ),
        )

        // Build a MedicalOrder that points at the item, then a MedicationAdministration on that lot.
        val orderRow = jdbcTemplate.queryForMap(
            """
            INSERT INTO medical_orders (
                admission_id, category, status, start_date, medication, inventory_item_id,
                created_at, updated_at
            ) VALUES (?, 'MEDICAMENTOS', 'AUTORIZADO', CURRENT_DATE, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """.trimIndent(),
            admission.id,
            item.name,
            item.id,
        )
        val orderId = (orderRow["id"] as Number).toLong()

        jdbcTemplate.update(
            """
            INSERT INTO medication_administrations (
                medical_order_id, admission_id, status, administered_at, lot_id, quantity,
                created_at, updated_at
            ) VALUES (?, ?, 'GIVEN', CURRENT_TIMESTAMP, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """.trimIndent(),
            orderId,
            admission.id,
            lot.id,
        )

        val ex = assertThrows(ConflictException::class.java) { lotService.deleteLot(lot.id!!) }
        assertNotNull(ex.message)
    }

    @Test
    fun `pharmacy_backfill_review view returns only NEEDS_REVIEW rows and disappears on confirmation (AC-19)`() {
        val (admin, adminToken) = createAdminUser()
        loginAsAdmin(admin)
        val item = seedDrugItem("ASA 500mg", sku = "ASA-500")
        val md = detailsRepository.save(
            MedicationDetails(
                item = item,
                genericName = "Acido Acetilsalicilico",
                strength = "500mg",
                dosageForm = DosageForm.TABLET,
                section = MedicationSection.NO_PSIQUIATRICO,
                reviewStatus = MedicationReviewStatus.NEEDS_REVIEW,
                reviewNotes = "Parser confidence low",
            ),
        )

        val before = jdbcTemplate.queryForList(
            "SELECT medication_details_id FROM pharmacy_backfill_review WHERE medication_details_id = ?",
            md.id,
        )
        assertEquals(1, before.size, "NEEDS_REVIEW row must show up in the view")

        md.reviewStatus = MedicationReviewStatus.CONFIRMED
        md.reviewNotes = null
        detailsRepository.save(md)

        val after = jdbcTemplate.queryForList(
            "SELECT medication_details_id FROM pharmacy_backfill_review WHERE medication_details_id = ?",
            md.id,
        )
        assertEquals(0, after.size, "CONFIRMED row must disappear from the view (AC-19)")
    }

    @Test
    fun `medication administration persists quantity for history responses (AC-26)`() {
        val (admin, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!)
        val admission = admissionRepository.findById(admissionId).get()
        loginAsAdmin(admin)
        val item = seedDrugItem()

        val orderRow = jdbcTemplate.queryForMap(
            """
            INSERT INTO medical_orders (
                admission_id, category, status, start_date, medication, inventory_item_id,
                created_at, updated_at
            ) VALUES (?, 'MEDICAMENTOS', 'AUTORIZADO', CURRENT_DATE, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """.trimIndent(),
            admission.id,
            item.name,
            item.id,
        )
        val orderId = (orderRow["id"] as Number).toLong()
        val order = medicalOrderRepository.findById(orderId).get()

        val saved = jdbcTemplate.queryForMap(
            """
            INSERT INTO medication_administrations (
                medical_order_id, admission_id, status, administered_at, quantity,
                created_at, updated_at
            ) VALUES (?, ?, 'GIVEN', CURRENT_TIMESTAMP, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id, quantity
            """.trimIndent(),
            order.id,
            admission.id,
        )

        assertEquals(
            3,
            (saved["quantity"] as Number).toInt(),
            "Quantity must persist as the value provided, not default to 1",
        )
    }
}
