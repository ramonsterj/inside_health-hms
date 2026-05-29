package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.entity.InventoryLot
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.UnprocessableEntityException
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.service.InventoryMovementService
import com.insidehealthgt.hms.service.PharmacyService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Warehouse-scoped dispensing coverage that the review flagged as missing:
 * AC-4 (nurse dispense is blocked when her own warehouse is empty even though
 * another warehouse holds stock), AC-5 (FEFO is computed per warehouse), and
 * AC-16 (two concurrent dispenses of the last unit do not oversell — the
 * `SELECT … FOR UPDATE` on the stock row serializes them) from
 * `docs/features/warehouse-inventory-management.md`.
 */
class WarehouseDispenseIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var lotRepository: InventoryLotRepository

    @Autowired
    private lateinit var pharmacyService: PharmacyService

    @Autowired
    private lateinit var movementService: InventoryMovementService

    private fun warehouseId(code: String): Long =
        jdbcTemplate.queryForObject("SELECT id FROM warehouses WHERE code = ?", Long::class.java, code)!!

    private fun loginAs(user: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(CustomUserDetails(user), null, emptyList())
    }

    private fun seedItem(name: String, kind: InventoryKind, lotTracked: Boolean): InventoryItem {
        val category = inventoryCategoryRepository.findAll().first { it.name == "Medicamentos" }
        return inventoryItemRepository.save(
            InventoryItem(
                category = category,
                name = name,
                description = null,
                price = BigDecimal("1.00"),
                cost = BigDecimal("0.50"),
                restockLevel = 0,
                pricingType = PricingType.FLAT,
                active = true,
                kind = kind,
                lotTrackingEnabled = lotTracked,
            ),
        )
    }

    private fun seedLot(item: InventoryItem, lotNumber: String, expiration: LocalDate): InventoryLot =
        lotRepository.save(
            InventoryLot(
                item = item,
                lotNumber = lotNumber,
                expirationDate = expiration,
                receivedAt = LocalDate.now(),
            ),
        )

    private fun seedStock(itemId: Long, warehouseCode: String, lotId: Long?, qty: Int) {
        jdbcTemplate.update(
            """
            INSERT INTO inventory_warehouse_stock (item_id, warehouse_id, lot_id, quantity, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """.trimIndent(),
            itemId,
            warehouseId(warehouseCode),
            lotId,
            qty,
        )
    }

    private fun stockIn(itemId: Long, warehouseCode: String): Int = jdbcTemplate.queryForObject(
        "SELECT COALESCE(SUM(quantity), 0) FROM inventory_warehouse_stock WHERE item_id = ? AND warehouse_id = ?",
        Int::class.java,
        itemId,
        warehouseId(warehouseCode),
    )!!

    private fun seedMedicationOrder(admissionId: Long, item: InventoryItem): Long {
        val row = jdbcTemplate.queryForMap(
            """
            INSERT INTO medical_orders (
                admission_id, category, status, start_date, medication, inventory_item_id,
                created_at, updated_at
            ) VALUES (?, 'MEDICAMENTOS', 'AUTORIZADO', CURRENT_DATE, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """.trimIndent(),
            admissionId,
            item.name,
            item.id,
        )
        return (row["id"] as Number).toLong()
    }

    @Test
    fun `AC-4 nurse dispense is blocked when ENFERMERIA is empty even though ADMINISTRACION has stock`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val (_, nurseToken) = createNurseUser()

        val item = seedItem("Risperidona 2 mg", InventoryKind.DRUG, lotTracked = true)
        val lot = seedLot(item, "LOT-AC4", LocalDate.now().plusYears(1))
        // 100 in ADMINISTRACION, nothing in ENFERMERIA (the nurse's warehouse).
        seedStock(item.id!!, "ADMINISTRACION", lot.id, 100)

        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!)
        val orderId = seedMedicationOrder(admissionId, item)

        val body = mapOf("status" to "GIVEN", "quantity" to 1)
        // ENFERMERIA empty -> 422 even though ADMINISTRACION holds 100.
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/administrations")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        ).andExpect(status().isUnprocessableEntity)

        assertEquals(
            100,
            stockIn(item.id!!, "ADMINISTRACION"),
            "ADMINISTRACION must be untouched on the failed dispense",
        )

        // Stock the nurse's warehouse and retry -> succeeds, debiting ENFERMERIA only.
        seedStock(item.id!!, "ENFERMERIA", lot.id, 5)
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/administrations")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("GIVEN"))

        assertEquals(4, stockIn(item.id!!, "ENFERMERIA"), "ENFERMERIA debited by 1")
        assertEquals(100, stockIn(item.id!!, "ADMINISTRACION"), "ADMINISTRACION still untouched")
    }

    @Test
    fun `AC-5 FEFO selects the earliest-expiring lot within the warehouse, not globally`() {
        val (admin, _) = createAdminUser()
        val item = seedItem("Clonazepam 2 mg", InventoryKind.DRUG, lotTracked = true)

        val lotA = seedLot(item, "LOT-A", LocalDate.now().plusDays(60))
        val lotB = seedLot(item, "LOT-B", LocalDate.now().plusDays(15))
        val lotC = seedLot(item, "LOT-C", LocalDate.now().plusDays(5))

        // ENFERMERIA holds A and B; ADMINISTRACION holds the globally-earliest C.
        seedStock(item.id!!, "ENFERMERIA", lotA.id, 10)
        seedStock(item.id!!, "ENFERMERIA", lotB.id, 10)
        seedStock(item.id!!, "ADMINISTRACION", lotC.id, 10)

        loginAs(admin)
        val preview = pharmacyService.fefoPreview(item.id!!, 1, warehouseId("ENFERMERIA"))

        assertNotNull(preview, "FEFO should find a lot in ENFERMERIA")
        assertEquals(
            lotB.id,
            preview!!.id,
            "FEFO must pick the earliest lot in ENFERMERIA (B), not the global earliest (C)",
        )
    }

    @Test
    fun `AC-18 medication detail exposes per-warehouse breakdown including empty warehouses`() {
        val (admin, _) = createAdminUser()
        loginAs(admin)

        val created = pharmacyService.createMedication(
            com.insidehealthgt.hms.dto.request.CreateMedicationRequest(
                name = "Quetiapina 100 mg",
                price = BigDecimal("2.00"),
                cost = BigDecimal("1.00"),
                genericName = "Quetiapina",
                dosageForm = com.insidehealthgt.hms.entity.DosageForm.TABLET,
                section = com.insidehealthgt.hms.entity.MedicationSection.PSIQUIATRICO,
            ),
        )
        val item = inventoryItemRepository.findById(created.itemId).orElseThrow()
        val lot = seedLot(item, "LOT-AC18", LocalDate.now().plusYears(1))
        // 50 in ADMINISTRACION, no row at all for ENFERMERIA (the nurse's warehouse).
        seedStock(item.id!!, "ADMINISTRACION", lot.id, 50)

        val detail = pharmacyService.getMedication(item.id!!)

        assertEquals(50, detail.quantity, "system-wide total sums all warehouses")
        // Breakdown lists every active warehouse (six seeded), including the empty ones.
        assertEquals(6, detail.warehouseStock.size, "breakdown covers all active warehouses")
        val adminWh = detail.warehouseStock.first { it.warehouseCode == "ADMINISTRACION" }
        val enfermeria = detail.warehouseStock.first { it.warehouseCode == "ENFERMERIA" }
        assertEquals(50, adminWh.quantity, "ADMINISTRACION on-hand")
        assertEquals(
            0,
            enfermeria.quantity,
            "ENFERMERIA must appear with 0 even though it has no stock row — so a nurse is not misled",
        )
    }

    @Test
    fun `AC-16 two concurrent dispenses of the last unit do not oversell`() {
        createAdminUser()
        val item = seedItem("Gasas esteriles", InventoryKind.SUPPLY, lotTracked = false)
        // Exactly one unit available in ENFERMERIA.
        seedStock(item.id!!, "ENFERMERIA", null, 1)

        val enfermeria = warehouseId("ENFERMERIA")
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)

        val task = Callable {
            // Align both threads so they contend for the same FOR UPDATE lock.
            barrier.await(5, TimeUnit.SECONDS)
            runCatching {
                movementService.createMovement(
                    item.id!!,
                    CreateInventoryMovementRequest(
                        type = MovementType.EXIT,
                        quantity = 1,
                        warehouseId = enfermeria,
                    ),
                )
            }
        }

        val results = try {
            listOf(pool.submit(task), pool.submit(task)).map { it.get(15, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        val successes = results.count { it.isSuccess }
        val failures = results.mapNotNull { it.exceptionOrNull() }

        assertEquals(1, successes, "exactly one concurrent dispense may succeed")
        assertEquals(1, failures.size, "the other must fail")
        assertEquals(
            UnprocessableEntityException::class.java,
            failures.first().javaClass,
            "the loser is rejected with the out-of-stock 422, not a generic error",
        )
        assertEquals(
            0,
            stockIn(item.id!!, "ENFERMERIA"),
            "stock must not go negative — only one unit left the warehouse",
        )
    }
}
