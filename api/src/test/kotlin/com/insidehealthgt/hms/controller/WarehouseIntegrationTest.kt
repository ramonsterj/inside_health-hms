package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryItemRequest
import com.insidehealthgt.hms.entity.InventoryKind
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

/**
 * Integration tests for the warehouse (bodega) feature: transfers, scope
 * isolation, maintenance charges, and warehouse CRUD guards. Covers AC-6, AC-7,
 * AC-8, AC-9, AC-10, AC-11, AC-13, AC-17 from warehouse-inventory-management.md.
 */
class WarehouseIntegrationTest : AbstractIntegrationTest() {

    private fun warehouseId(code: String): Long =
        jdbcTemplate.queryForObject("SELECT id FROM warehouses WHERE code = ?", Long::class.java, code)!!

    private fun testCategoryId(): Long {
        jdbcTemplate.update(
            """
            INSERT INTO inventory_categories (name, display_order, active, created_at, updated_at)
            VALUES ('WH-TEST', 99, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (name) WHERE deleted_at IS NULL DO NOTHING
            """.trimIndent(),
        )
        return jdbcTemplate.queryForObject(
            "SELECT id FROM inventory_categories WHERE name = 'WH-TEST'",
            Long::class.java,
        )!!
    }

    /** Creates a non-lot-tracked SUPPLY item via the admin API. */
    private fun createSupplyItem(adminToken: String, price: BigDecimal = BigDecimal("75.00")): Long {
        val request = CreateInventoryItemRequest(
            name = "Toalla de baño",
            categoryId = testCategoryId(),
            price = price,
            cost = BigDecimal("10.00"),
            restockLevel = 5,
            kind = InventoryKind.SUPPLY,
        )
        val result = mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()
        check(result.response.status == 201) { "item create failed: ${result.response.contentAsString}" }
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    /** Admin receives [qty] of [itemId] into [warehouseCode] via an ENTRY movement. */
    private fun enterStock(adminToken: String, itemId: Long, warehouseCode: String, qty: Int) {
        val request = mapOf("type" to "ENTRY", "quantity" to qty, "warehouseId" to warehouseId(warehouseCode))
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    private fun stockIn(itemId: Long, warehouseCode: String): Int = jdbcTemplate.queryForObject(
        "SELECT COALESCE(SUM(quantity), 0) FROM inventory_warehouse_stock WHERE item_id = ? AND warehouse_id = ?",
        Int::class.java,
        itemId,
        warehouseId(warehouseCode),
    )!!

    @Test
    fun `AC-6 transfer happy path moves stock and writes transfer plus two movements`() {
        val (_, adminToken) = createAdminUser()
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "ADMINISTRACION", 50)

        val transfer = mapOf(
            "sourceWarehouseId" to warehouseId("ADMINISTRACION"),
            "destinationWarehouseId" to warehouseId("ENFERMERIA"),
            "itemId" to itemId,
            "quantity" to 20,
            "notes" to "Restock for the night shift",
        )
        mockMvc.perform(
            post("/api/v1/warehouse-transfers")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.quantity").value(20))

        assert(stockIn(itemId, "ADMINISTRACION") == 30) { "source should be 30" }
        assert(stockIn(itemId, "ENFERMERIA") == 20) { "destination should be 20" }

        val transferRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_transfers WHERE item_id = ? AND status = 'COMPLETED'",
            Int::class.java,
            itemId,
        )!!
        assert(transferRows == 1) { "exactly one transfer row" }

        val movementRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_movements WHERE item_id = ? AND transfer_id IS NOT NULL",
            Int::class.java,
            itemId,
        )!!
        assert(movementRows == 2) { "two movements (EXIT + ENTRY) linked to the transfer" }

        // AC-17: a WAREHOUSE_TRANSFER audit row was written.
        val auditRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_logs WHERE action = 'WAREHOUSE_TRANSFER'",
            Int::class.java,
        )!!
        assert(auditRows >= 1) { "transfer audit row written" }
    }

    @Test
    fun `AC-7 transfer over source stock returns 422 and writes nothing`() {
        val (_, adminToken) = createAdminUser()
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "ADMINISTRACION", 50)

        val transfer = mapOf(
            "sourceWarehouseId" to warehouseId("ADMINISTRACION"),
            "destinationWarehouseId" to warehouseId("ENFERMERIA"),
            "itemId" to itemId,
            "quantity" to 100,
        )
        mockMvc.perform(
            post("/api/v1/warehouse-transfers")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer)),
        ).andExpect(status().isUnprocessableEntity)

        assert(stockIn(itemId, "ADMINISTRACION") == 50) { "source untouched" }
        val transferRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory_transfers WHERE item_id = ?",
            Int::class.java,
            itemId,
        )!!
        assert(transferRows == 0) { "no transfer row written on failure" }
    }

    @Test
    fun `AC-8 nurse cannot transfer from a warehouse she does not own`() {
        val (_, adminToken) = createAdminUser()
        val (_, nurseToken) = createNurseUser()
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "ADMINISTRACION", 50)

        val transfer = mapOf(
            "sourceWarehouseId" to warehouseId("ADMINISTRACION"),
            "destinationWarehouseId" to warehouseId("ENFERMERIA"),
            "itemId" to itemId,
            "quantity" to 5,
        )
        mockMvc.perform(
            post("/api/v1/warehouse-transfers")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer)),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `AC-9 maintenance user charges a consumable and a SERVICE patient charge is created`() {
        val (_, adminToken) = createAdminUser()
        val (maint, maintToken) = createUserWithRole(
            roleCode = "MAINTENANCE",
            username = "maint",
            email = "maint@example.com",
            password = "password123",
        )
        // Assign MANTENIMIENTO_1 to the maintenance user.
        jdbcTemplate.update(
            "INSERT INTO user_warehouses (user_id, warehouse_id, created_at, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            maint.id,
            warehouseId("MANTENIMIENTO_1"),
        )

        val (doctor, _) = createDoctorUser()
        val itemId = createSupplyItem(adminToken, price = BigDecimal("75.00"))
        enterStock(adminToken, itemId, "MANTENIMIENTO_1", 10)

        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!)

        val charge = mapOf(
            "warehouseId" to warehouseId("MANTENIMIENTO_1"),
            "itemId" to itemId,
            "admissionId" to admissionId,
            "quantity" to 1,
            "reason" to "Manchada — el paciente la pintó",
        )
        mockMvc.perform(
            post("/api/v1/warehouse-charges")
                .header("Authorization", "Bearer $maintToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(charge)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.amount").value(75.00))
            .andExpect(jsonPath("$.data.quantity").value(1))

        assert(stockIn(itemId, "MANTENIMIENTO_1") == 9) { "warehouse debited by 1" }

        // The billing listener (AFTER_COMMIT) creates a SERVICE patient charge.
        val chargeRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM patient_charges WHERE admission_id = ? AND charge_type = 'SERVICE'",
            Int::class.java,
            admissionId,
        )!!
        assert(chargeRows == 1) { "one SERVICE patient charge created from the warehouse charge" }
    }

    @Test
    fun `AC-10 nurse lacks warehouse-charge create and is forbidden`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val (_, nurseToken) = createNurseUser()
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "ENFERMERIA", 10)
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!)

        val charge = mapOf(
            "warehouseId" to warehouseId("ENFERMERIA"),
            "itemId" to itemId,
            "admissionId" to admissionId,
            "quantity" to 1,
            "reason" to "test",
        )
        mockMvc.perform(
            post("/api/v1/warehouse-charges")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(charge)),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `AC-11 warehouse holding stock cannot be deleted`() {
        val (_, adminToken) = createAdminUser()
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "COCINA", 5)

        mockMvc.perform(
            delete("/api/v1/warehouses/${warehouseId("COCINA")}")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `AC-13 maintenance user can view assigned warehouse stock but is denied an unassigned one`() {
        createAdminUser()
        val (maint, maintToken) = createUserWithRole(
            roleCode = "MAINTENANCE",
            username = "maint",
            email = "maint@example.com",
            password = "password123",
        )
        // Assign only MANTENIMIENTO_1.
        jdbcTemplate.update(
            "INSERT INTO user_warehouses (user_id, warehouse_id, created_at, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            maint.id,
            warehouseId("MANTENIMIENTO_1"),
        )

        // Own warehouse: allowed.
        mockMvc.perform(
            get("/api/v1/warehouses/${warehouseId("MANTENIMIENTO_1")}/stock")
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isOk)

        // Unassigned warehouse: forbidden.
        mockMvc.perform(
            get("/api/v1/warehouses/${warehouseId("MANTENIMIENTO_2")}/stock")
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `AC-13 single warehouse get is scoped - maintenance user denied an unassigned warehouse`() {
        createAdminUser()
        val (maint, maintToken) = createUserWithRole(
            roleCode = "MAINTENANCE",
            username = "maint",
            email = "maint@example.com",
            password = "password123",
        )
        jdbcTemplate.update(
            "INSERT INTO user_warehouses (user_id, warehouse_id, created_at, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            maint.id,
            warehouseId("MANTENIMIENTO_1"),
        )

        // Own warehouse: allowed.
        mockMvc.perform(
            get("/api/v1/warehouses/${warehouseId("MANTENIMIENTO_1")}")
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isOk)

        // Unassigned warehouse: forbidden (must not leak via the single-resource endpoint).
        mockMvc.perform(
            get("/api/v1/warehouses/${warehouseId("MANTENIMIENTO_2")}")
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `warehouse list is scoped - nurse sees only her enfermeria warehouse`() {
        createAdminUser()
        val (_, nurseToken) = createNurseUser()

        mockMvc.perform(
            get("/api/v1/warehouses").header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].code").value("ENFERMERIA"))
    }

    @Test
    fun `AC-13 catalog list and low-stock filtered to a warehouse enforce view scope`() {
        val (_, adminToken) = createAdminUser()
        val (maint, maintToken) = createUserWithRole(
            roleCode = "MAINTENANCE",
            username = "maint",
            email = "maint@example.com",
            password = "password123",
        )
        // Assign only MANTENIMIENTO_1.
        jdbcTemplate.update(
            "INSERT INTO user_warehouses (user_id, warehouse_id, created_at, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            maint.id,
            warehouseId("MANTENIMIENTO_1"),
        )
        val itemId = createSupplyItem(adminToken)
        enterStock(adminToken, itemId, "MANTENIMIENTO_1", 10)

        // Assigned warehouse: allowed.
        mockMvc.perform(
            get("/api/v1/admin/inventory-items")
                .param("warehouseId", warehouseId("MANTENIMIENTO_1").toString())
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isOk)

        // Unassigned warehouse: forbidden — cannot read another bodega's per-item
        // stock by passing its id, even though MAINTENANCE holds inventory-item:read.
        mockMvc.perform(
            get("/api/v1/admin/inventory-items")
                .param("warehouseId", warehouseId("MANTENIMIENTO_2").toString())
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isForbidden)

        // Low-stock report is scoped the same way (FR-7).
        mockMvc.perform(
            get("/api/v1/admin/inventory-items/low-stock")
                .param("warehouseId", warehouseId("MANTENIMIENTO_2").toString())
                .header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isForbidden)

        // System-wide list (no warehouseId) stays open to any inventory-item:read holder.
        mockMvc.perform(
            get("/api/v1/admin/inventory-items").header("Authorization", "Bearer $maintToken"),
        ).andExpect(status().isOk)
    }
}
