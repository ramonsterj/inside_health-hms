package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.request.CreateInventoryItemRequest
import com.insidehealthgt.hms.dto.request.CreateInventoryMovementRequest
import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdateInventoryItemRequest
import com.insidehealthgt.hms.dto.request.UpdateRoomRequest
import com.insidehealthgt.hms.entity.MovementType
import com.insidehealthgt.hms.entity.PricingType
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.TimeUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@Suppress("LargeClass")
class InventoryControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, docTkn) = createDoctorUser()
        doctorToken = docTkn
    }

    // ============ CATEGORY TESTS ============

    @Test
    fun `list all categories should return seeded categories`() {
        mockMvc.perform(
            get("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(7))
            .andExpect(jsonPath("$.data[0].name").value("Medicamentos"))
    }

    @Test
    fun `list active categories should return seeded categories`() {
        mockMvc.perform(
            get("/api/v1/inventory-categories")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `create category with valid data should return 201`() {
        val request = CreateInventoryCategoryRequest(
            name = "Test Category",
            description = "Test description",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Test Category"))
            .andExpect(jsonPath("$.data.description").value("Test description"))
            .andExpect(jsonPath("$.data.displayOrder").value(50))
            .andExpect(jsonPath("$.data.active").value(true))
    }

    @Test
    fun `create category should fail with duplicate name`() {
        val request = CreateInventoryCategoryRequest(
            name = "Medicamentos",
            description = "Test",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create category should fail with blank name`() {
        val request = mapOf(
            "name" to "",
            "description" to "Test",
            "displayOrder" to 50,
            "active" to true,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update category should update data`() {
        val createRequest = CreateInventoryCategoryRequest(
            name = "Original Name",
            description = "Original description",
            displayOrder = 50,
            active = true,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val categoryId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateInventoryCategoryRequest(
            name = "Updated Name",
            description = "Updated description",
            displayOrder = 60,
            active = false,
        )

        mockMvc.perform(
            put("/api/v1/admin/inventory-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("Updated Name"))
            .andExpect(jsonPath("$.data.description").value("Updated description"))
            .andExpect(jsonPath("$.data.displayOrder").value(60))
            .andExpect(jsonPath("$.data.active").value(false))
    }

    @Test
    fun `delete unused category should return 204`() {
        val createRequest = CreateInventoryCategoryRequest(
            name = "To Delete",
            description = "Will be deleted",
            displayOrder = 99,
            active = true,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val categoryId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admin/inventory-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/v1/admin/inventory-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete category with items should return 400`() {
        val categoryId = getSeededCategoryId()

        // Create an item in this category
        val itemRequest = CreateInventoryItemRequest(
            name = "Test Item",
            categoryId = categoryId,
            price = BigDecimal("10.00"),
            cost = BigDecimal("5.00"),
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemRequest)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            delete("/api/v1/admin/inventory-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `category endpoints should fail for non-admin`() {
        mockMvc.perform(
            get("/api/v1/admin/inventory-categories")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ ITEM TESTS ============

    @Test
    fun `create flat-priced item should return 201`() {
        val categoryId = getSeededCategoryId()

        val request = CreateInventoryItemRequest(
            name = "Aspirina 500mg",
            description = "Aspirin tablets 500mg",
            categoryId = categoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 100,
            pricingType = PricingType.FLAT,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.name").value("Aspirina 500mg"))
            .andExpect(jsonPath("$.data.price").value(5.00))
            .andExpect(jsonPath("$.data.cost").value(2.50))
            .andExpect(jsonPath("$.data.quantity").value(0))
            .andExpect(jsonPath("$.data.restockLevel").value(100))
            .andExpect(jsonPath("$.data.pricingType").value("FLAT"))
            .andExpect(jsonPath("$.data.timeUnit").doesNotExist())
            .andExpect(jsonPath("$.data.timeInterval").doesNotExist())
            .andExpect(jsonPath("$.data.category.name").value("Medicamentos"))
    }

    @Test
    fun `create time-based item should return 201`() {
        val categoryId = getSeededCategoryId()

        val request = CreateInventoryItemRequest(
            name = "Heart Rate Monitor",
            description = "Heart rate monitor for patient monitoring",
            categoryId = categoryId,
            price = BigDecimal("50.00"),
            cost = BigDecimal("10.00"),
            restockLevel = 5,
            pricingType = PricingType.TIME_BASED,
            timeUnit = TimeUnit.HOURS,
            timeInterval = 1,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.pricingType").value("TIME_BASED"))
            .andExpect(jsonPath("$.data.timeUnit").value("HOURS"))
            .andExpect(jsonPath("$.data.timeInterval").value(1))
    }

    @Test
    fun `create time-based item without time unit should fail`() {
        val categoryId = getSeededCategoryId()

        val request = CreateInventoryItemRequest(
            name = "Monitor",
            categoryId = categoryId,
            price = BigDecimal("50.00"),
            cost = BigDecimal("10.00"),
            pricingType = PricingType.TIME_BASED,
            timeUnit = null,
            timeInterval = 1,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update item should not change quantity`() {
        val itemId = createTestItem()

        // Add stock first
        val movementRequest = CreateInventoryMovementRequest(
            type = MovementType.ENTRY,
            quantity = 50,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(movementRequest)),
        ).andExpect(status().isCreated)

        // Update item
        val updateRequest = UpdateInventoryItemRequest(
            name = "Updated Item",
            categoryId = getSeededCategoryId(),
            price = BigDecimal("20.00"),
            cost = BigDecimal("8.00"),
            restockLevel = 10,
        )

        mockMvc.perform(
            put("/api/v1/admin/inventory-items/$itemId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("Updated Item"))
            .andExpect(jsonPath("$.data.price").value(20.00))
            .andExpect(jsonPath("$.data.quantity").value(50))
    }

    @Test
    fun `delete item should soft delete`() {
        val itemId = createTestItem()

        mockMvc.perform(
            delete("/api/v1/admin/inventory-items/$itemId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/v1/admin/inventory-items/$itemId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `list items should return paginated results`() {
        createTestItem()

        mockMvc.perform(
            get("/api/v1/admin/inventory-items?page=0&size=10")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.page.totalElements").value(1))
            .andExpect(jsonPath("$.data.page.size").value(10))
    }

    @Test
    fun `filter items by category should return matching items`() {
        val categoryId = getSeededCategoryId()
        createTestItem()

        mockMvc.perform(
            get("/api/v1/admin/inventory-items?categoryId=$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    @Test
    fun `search items by name should return matching items`() {
        createTestItem()

        mockMvc.perform(
            get("/api/v1/admin/inventory-items?search=aspir")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("Aspirina 500mg"))
    }

    @Test
    fun `item endpoints should fail for non-admin`() {
        mockMvc.perform(
            get("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ MOVEMENT TESTS ============

    @Test
    fun `ENTRY movement should increase quantity`() {
        val itemId = createTestItem()

        val request = CreateInventoryMovementRequest(
            type = MovementType.ENTRY,
            quantity = 50,
            notes = "Monthly restock",
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.type").value("ENTRY"))
            .andExpect(jsonPath("$.data.quantity").value(50))
            .andExpect(jsonPath("$.data.previousQuantity").value(0))
            .andExpect(jsonPath("$.data.newQuantity").value(50))
            .andExpect(jsonPath("$.data.notes").value("Monthly restock"))

        // Verify item quantity
        mockMvc.perform(
            get("/api/v1/admin/inventory-items/$itemId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(jsonPath("$.data.quantity").value(50))
    }

    @Test
    fun `EXIT movement should decrease quantity`() {
        val itemId = createTestItem()

        // First add stock
        val entryRequest = CreateInventoryMovementRequest(
            type = MovementType.ENTRY,
            quantity = 50,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entryRequest)),
        ).andExpect(status().isCreated)

        // Then remove stock
        val exitRequest = CreateInventoryMovementRequest(
            type = MovementType.EXIT,
            quantity = 20,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exitRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.type").value("EXIT"))
            .andExpect(jsonPath("$.data.previousQuantity").value(50))
            .andExpect(jsonPath("$.data.newQuantity").value(30))

        // Verify item quantity
        mockMvc.perform(
            get("/api/v1/admin/inventory-items/$itemId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(jsonPath("$.data.quantity").value(30))
    }

    @Test
    fun `EXIT with insufficient stock should return 400`() {
        val itemId = createTestItem()

        // Add 10 units
        val entryRequest = CreateInventoryMovementRequest(
            type = MovementType.ENTRY,
            quantity = 10,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entryRequest)),
        ).andExpect(status().isCreated)

        // Try to remove 100
        val exitRequest = CreateInventoryMovementRequest(
            type = MovementType.EXIT,
            quantity = 100,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exitRequest)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `movement with quantity 0 should fail validation`() {
        val itemId = createTestItem()

        val request = mapOf(
            "type" to "ENTRY",
            "quantity" to 0,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `movement history should be sorted DESC`() {
        val itemId = createTestItem()

        // Create two movements
        val entry1 = CreateInventoryMovementRequest(type = MovementType.ENTRY, quantity = 10)
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entry1)),
        ).andExpect(status().isCreated)

        val entry2 = CreateInventoryMovementRequest(type = MovementType.ENTRY, quantity = 20, notes = "Second")
        mockMvc.perform(
            post("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entry2)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/admin/inventory-items/$itemId/movements")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].notes").value("Second"))
    }

    // ============ LOW STOCK TESTS ============

    @Test
    fun `low stock report should return items below restock level`() {
        val categoryId = getSeededCategoryId()

        // Create item with restock level > 0 (quantity starts at 0, so it's low stock)
        val request = CreateInventoryItemRequest(
            name = "Low Stock Item",
            categoryId = categoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 100,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/admin/inventory-items/low-stock")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Low Stock Item"))
    }

    @Test
    fun `low stock report should filter by category`() {
        val categoryId = getSeededCategoryId()
        val secondCategoryId = getSeededCategoryId(1) // Laboratorios or similar

        // Create item in first category
        val request1 = CreateInventoryItemRequest(
            name = "Low Stock A",
            categoryId = categoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 100,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andExpect(status().isCreated)

        // Create item in second category
        val request2 = CreateInventoryItemRequest(
            name = "Low Stock B",
            categoryId = secondCategoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 50,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        // Filter by first category
        mockMvc.perform(
            get("/api/v1/admin/inventory-items/low-stock?categoryId=$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Low Stock A"))
    }

    @Test
    fun `low stock report should exclude items with restock level 0`() {
        val categoryId = getSeededCategoryId()

        // Create item with restockLevel=0
        val request = CreateInventoryItemRequest(
            name = "No Threshold Item",
            categoryId = categoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 0,
        )
        mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/admin/inventory-items/low-stock")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    // ============ ROOM PRICING TESTS ============

    @Test
    fun `create room with price and cost should return pricing`() {
        val request = CreateRoomRequest(
            number = "101",
            type = RoomType.PRIVATE,
            gender = RoomGender.FEMALE,
            capacity = 1,
            price = BigDecimal("1500.00"),
            cost = BigDecimal("800.00"),
        )

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.price").value(1500.00))
            .andExpect(jsonPath("$.data.cost").value(800.00))
    }

    @Test
    fun `update room with price and cost should save pricing`() {
        // Create room first
        val createRequest = CreateRoomRequest(
            number = "102",
            type = RoomType.SHARED,
            gender = RoomGender.MALE,
            capacity = 2,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateRoomRequest(
            number = "102",
            type = RoomType.SHARED,
            gender = RoomGender.MALE,
            capacity = 2,
            price = BigDecimal("2000.00"),
            cost = BigDecimal("1000.00"),
        )

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.price").value(2000.00))
            .andExpect(jsonPath("$.data.cost").value(1000.00))
    }

    @Test
    fun `create room with negative price should return 400`() {
        val request = mapOf(
            "number" to "103",
            "type" to "PRIVATE",
            "gender" to "FEMALE",
            "capacity" to 1,
            "price" to -100,
            "cost" to 800,
        )

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ HELPERS ============

    private fun getSeededCategoryId(offset: Int = 0): Long {
        val categories = inventoryCategoryRepository.findAllByOrderByDisplayOrderAsc()
        return categories[offset].id!!
    }

    private fun createTestItem(): Long {
        val categoryId = getSeededCategoryId()

        val request = CreateInventoryItemRequest(
            name = "Aspirina 500mg",
            description = "Aspirin tablets",
            categoryId = categoryId,
            price = BigDecimal("5.00"),
            cost = BigDecimal("2.50"),
            restockLevel = 100,
        )

        val result = mockMvc.perform(
            post("/api/v1/admin/inventory-items")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
