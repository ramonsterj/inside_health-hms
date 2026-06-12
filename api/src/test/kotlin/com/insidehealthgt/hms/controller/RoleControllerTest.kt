@file:Suppress("FunctionSignature", "FunctionExpressionBody")

package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AssignPermissionsRequest
import com.insidehealthgt.hms.dto.request.CreateRoleRequest
import com.insidehealthgt.hms.dto.request.UpdateRoleRequest
import com.insidehealthgt.hms.security.SystemRole
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class RoleControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String

    @BeforeEach
    fun setUp() {
        adminToken = createAdminUser().second
    }

    private fun systemRoleId(code: String): Long = roleRepository.findByCode(code)!!.id!!

    private fun createCustomRole(code: String, name: String): Long {
        val request = CreateRoleRequest(code = code, name = name, description = "Custom role")
        val result = mockMvc.perform(
            post("/api/roles")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    // ============ SYSTEM-ROLE LOCK ============

    @Test
    fun `assigning permissions to a system role is rejected`() {
        val request = AssignPermissionsRequest(permissionCodes = listOf("user:read"))
        mockMvc.perform(
            put("/api/roles/${systemRoleId(SystemRole.ENFERMERO)}/permissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `updating a system role is rejected`() {
        val request = UpdateRoleRequest(name = "Renamed")
        mockMvc.perform(
            put("/api/roles/${systemRoleId(SystemRole.ENFERMERO)}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `deleting a system role is rejected`() {
        mockMvc.perform(
            delete("/api/roles/${systemRoleId(SystemRole.ENFERMERO)}")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isBadRequest)
    }

    // ============ CUSTOM ROLE STILL EDITABLE ============

    @Test
    fun `assigning permissions to a custom role succeeds and echoes them`() {
        val roleId = createCustomRole("CUSTOM_REPORTER", "Custom Reporter")
        val request = AssignPermissionsRequest(permissionCodes = listOf("user:read"))
        mockMvc.perform(
            put("/api/roles/$roleId/permissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.permissions[*].code").value(hasItem("user:read")))
    }

    // ============ AUTHORIZATION ============

    @Test
    fun `non-privileged user cannot assign permissions`() {
        val nurseToken = createNurseUser().second
        val request = AssignPermissionsRequest(permissionCodes = listOf("user:read"))
        mockMvc.perform(
            put("/api/roles/${systemRoleId(SystemRole.ENFERMERO)}/permissions")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isForbidden)
    }
}
