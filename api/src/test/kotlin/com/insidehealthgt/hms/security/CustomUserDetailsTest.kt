package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.entity.Permission
import com.insidehealthgt.hms.entity.Role
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomUserDetailsTest {

    private fun createUserWithRolesAndPermissions(
        status: UserStatus = UserStatus.ACTIVE,
        roleCodes: List<String> = emptyList(),
        permissionCodes: List<String> = emptyList(),
    ): User {
        val permissions = permissionCodes.mapIndexed { index, code ->
            Permission(
                code = code,
                name = code,
                resource = code.substringBefore(":"),
                action = code.substringAfter(":"),
            ).apply { id = (index + 1).toLong() }
        }.toMutableSet()

        val roles = roleCodes.mapIndexed { index, code ->
            Role(
                code = code,
                name = code,
                permissions = permissions,
            ).apply { id = (index + 1).toLong() }
        }.toMutableSet()

        return User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            status = status,
        ).apply {
            id = 1L
            this.roles = roles
        }
    }

    @Test
    fun `getAuthorities should include ROLE_ prefixed roles and permissions`() {
        val user = createUserWithRolesAndPermissions(
            roleCodes = listOf("ADMINISTRADOR", "MEDICO"),
            permissionCodes = listOf("user:create", "patient:read"),
        )
        val userDetails = CustomUserDetails(user)

        val authorities = userDetails.authorities.map { it.authority }.toSet()

        assertTrue(authorities.contains("ROLE_ADMINISTRADOR"))
        assertTrue(authorities.contains("ROLE_MEDICO"))
        assertTrue(authorities.contains("user:create"))
        assertTrue(authorities.contains("patient:read"))
        assertEquals(4, authorities.size)
    }

    @Test
    fun `isEnabled should return true for ACTIVE status`() {
        val user = createUserWithRolesAndPermissions(status = UserStatus.ACTIVE)
        val userDetails = CustomUserDetails(user)

        assertTrue(userDetails.isEnabled)
    }

    @Test
    fun `isEnabled should return false for INACTIVE status`() {
        val user = createUserWithRolesAndPermissions(status = UserStatus.INACTIVE)
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isEnabled)
    }

    @Test
    fun `isEnabled should return false for DELETED status`() {
        val user = createUserWithRolesAndPermissions(status = UserStatus.DELETED)
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isEnabled)
    }

    @Test
    fun `isAccountNonLocked should return false for SUSPENDED status`() {
        val user = createUserWithRolesAndPermissions(status = UserStatus.SUSPENDED)
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isAccountNonLocked)
    }

    @Test
    fun `isAccountNonLocked should return true for ACTIVE status`() {
        val user = createUserWithRolesAndPermissions(status = UserStatus.ACTIVE)
        val userDetails = CustomUserDetails(user)

        assertTrue(userDetails.isAccountNonLocked)
    }

    @Test
    fun `hasRole should return true for assigned role`() {
        val user = createUserWithRolesAndPermissions(roleCodes = listOf("ADMINISTRADOR"))
        val userDetails = CustomUserDetails(user)

        assertTrue(userDetails.hasRole("ADMINISTRADOR"))
        assertFalse(userDetails.hasRole("MEDICO"))
    }

    @Test
    fun `isAuxiliaryNurseOnly is true when the only role is AUXILIAR_ENFERMERIA`() {
        val user = createUserWithRolesAndPermissions(roleCodes = listOf("AUXILIAR_ENFERMERIA"))
        val userDetails = CustomUserDetails(user)

        assertTrue(userDetails.isAuxiliaryNurseOnly())
    }

    @Test
    fun `isAuxiliaryNurseOnly is false for a plain ENFERMERO`() {
        val user = createUserWithRolesAndPermissions(roleCodes = listOf("ENFERMERO"))
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isAuxiliaryNurseOnly())
    }

    @Test
    fun `isAuxiliaryNurseOnly is false when AUXILIAR_ENFERMERIA is stacked with ENFERMERO`() {
        val user = createUserWithRolesAndPermissions(roleCodes = listOf("ENFERMERO", "AUXILIAR_ENFERMERIA"))
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isAuxiliaryNurseOnly())
    }

    @Test
    fun `isAuxiliaryNurseOnly is false when AUXILIAR_ENFERMERIA is stacked with an elevated role`() {
        listOf("JEFE_ENFERMERIA", "MEDICO", "MEDICO_RESIDENTE", "ADMINISTRADOR").forEach { elevatedRole ->
            val user = createUserWithRolesAndPermissions(roleCodes = listOf("AUXILIAR_ENFERMERIA", elevatedRole))
            assertFalse(
                CustomUserDetails(user).isAuxiliaryNurseOnly(),
                "AUXILIAR_ENFERMERIA stacked with $elevatedRole must not be treated as auxiliary-only",
            )
        }
    }

    @Test
    fun `isAuxiliaryNurseOnly is false when the user has no AUXILIAR_ENFERMERIA role`() {
        val user = createUserWithRolesAndPermissions(roleCodes = listOf("MEDICO"))
        val userDetails = CustomUserDetails(user)

        assertFalse(userDetails.isAuxiliaryNurseOnly())
    }

    @Test
    fun `hasPermission should return true for assigned permission`() {
        val user = createUserWithRolesAndPermissions(
            roleCodes = listOf("ADMINISTRADOR"),
            permissionCodes = listOf("user:create", "user:delete"),
        )
        val userDetails = CustomUserDetails(user)

        assertTrue(userDetails.hasPermission("user:create"))
        assertTrue(userDetails.hasPermission("user:delete"))
        assertFalse(userDetails.hasPermission("patient:create"))
    }
}
