package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.Role
import com.insidehealthgt.hms.entity.RoleDefaultWarehouse
import com.insidehealthgt.hms.entity.UserWarehouse
import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.UnprocessableEntityException
import com.insidehealthgt.hms.repository.RoleDefaultWarehouseRepository
import com.insidehealthgt.hms.repository.UserWarehouseRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WarehouseScopeServiceTest {

    private lateinit var warehouseRepository: WarehouseRepository
    private lateinit var userWarehouseRepository: UserWarehouseRepository
    private lateinit var roleDefaultWarehouseRepository: RoleDefaultWarehouseRepository
    private lateinit var scopeService: WarehouseScopeService

    private fun warehouse(id: Long, code: String) = Warehouse(code = code, name = code).apply { this.id = id }

    private val enfermeria = warehouse(2, "ENFERMERIA")
    private val administracion = warehouse(1, "ADMINISTRACION")
    private val mant1 = warehouse(3, "MANTENIMIENTO_1")

    private fun user(id: Long, vararg roles: String): CustomUserDetails = mock {
        on { getRoleCodes() } doReturn roles.toSet()
        on { this.id } doReturn id
    }

    @BeforeEach
    fun setUp() {
        warehouseRepository = mock()
        userWarehouseRepository = mock()
        roleDefaultWarehouseRepository = mock()
        val messageService = mock<MessageService> {
            on { errorWarehouseUnassigned() } doReturn "unassigned"
            on { errorWarehouseTransferSourceDenied() } doReturn "source denied"
            on { errorWarehouseViewDenied() } doReturn "view denied"
        }
        scopeService = WarehouseScopeService(
            warehouseRepository,
            userWarehouseRepository,
            roleDefaultWarehouseRepository,
            messageService,
        )
        whenever(roleDefaultWarehouseRepository.findByRoleCodes(any())).thenReturn(emptyList())
        whenever(userWarehouseRepository.findByUserId(any())).thenReturn(emptyList())
    }

    @Test
    fun `nurse resolves to her default ENFERMERIA via the lookup table`() {
        whenever(roleDefaultWarehouseRepository.findByRoleCodes(any()))
            .thenReturn(listOf(RoleDefaultWarehouse(role = Role(code = "NURSE", name = "n"), warehouse = enfermeria)))

        val resolved = scopeService.resolveDispensingWarehouse(user(1, "NURSE"))

        assertEquals("ENFERMERIA", resolved.code)
    }

    @Test
    fun `admin without a mapping falls back to ENFERMERIA`() {
        whenever(warehouseRepository.findByCode("ENFERMERIA")).thenReturn(enfermeria)

        val resolved = scopeService.resolveDispensingWarehouse(user(1, "ADMIN"))

        assertEquals("ENFERMERIA", resolved.code)
    }

    @Test
    fun `maintenance user resolves to their single assigned warehouse`() {
        whenever(userWarehouseRepository.findByUserId(9))
            .thenReturn(listOf(UserWarehouse(user = mock(), warehouse = mant1)))

        val resolved = scopeService.resolveDispensingWarehouse(user(9, "MAINTENANCE"))

        assertEquals("MANTENIMIENTO_1", resolved.code)
    }

    @Test
    fun `unassigned user with no mapping throws 422`() {
        assertThrows(UnprocessableEntityException::class.java) {
            scopeService.resolveDispensingWarehouse(user(5, "USER"))
        }
    }

    @Test
    fun `admin may use any warehouse as a transfer source`() {
        scopeService.assertCanUseAsSource(user(1, "ADMIN"), administracion)
    }

    @Test
    fun `nurse may use her own ENFERMERIA as source but not ADMINISTRACION`() {
        whenever(roleDefaultWarehouseRepository.findByRoleCodes(any()))
            .thenReturn(listOf(RoleDefaultWarehouse(role = Role(code = "NURSE", name = "n"), warehouse = enfermeria)))
        val nurse = user(1, "NURSE")

        scopeService.assertCanUseAsSource(nurse, enfermeria) // owned — no throw

        assertThrows(ForbiddenException::class.java) {
            scopeService.assertCanUseAsSource(nurse, administracion)
        }
    }
}
