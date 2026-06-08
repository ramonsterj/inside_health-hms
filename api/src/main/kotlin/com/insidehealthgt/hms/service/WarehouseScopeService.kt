package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.Warehouse
import com.insidehealthgt.hms.entity.WarehouseCodes
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnprocessableEntityException
import com.insidehealthgt.hms.repository.RoleDefaultWarehouseRepository
import com.insidehealthgt.hms.repository.UserWarehouseRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The heart of warehouse isolation. Resolves which bodega an implicit operation
 * acts on, and enforces who may use a warehouse as a transfer/charge source or
 * see it in the stock view.
 *
 * ## Resolution precedence for an implicit dispensing warehouse (FR-9)
 *
 * [resolveDispensingWarehouse] consults, in order:
 *  1. The data-driven role -> default-warehouse mapping (`role_default_warehouses`).
 *     ENFERMERO / AUXILIAR_ENFERMERIA / JEFE_ENFERMERIA -> ENFERMERIA, PSICOLOGO -> PSICOLOGIA.
 *     A single mapped warehouse wins; if several map (rare stacked roles) ENFERMERIA
 *     is preferred because that is the canonical medication-dispensing bodega.
 *  2. Otherwise, ADMINISTRADOR / PERSONAL_ADMINISTRATIVO / MEDICO / MEDICO_RESIDENTE fall back to
 *     ENFERMERIA — these roles dispense (or test-dispense) into the nursing bodega
 *     unless a caller passes an explicit warehouse elsewhere.
 *  3. Otherwise a MANTENIMIENTO user resolves to their single `user_warehouses`
 *     assignment (ambiguous when they own several -> 422).
 *  4. Otherwise 422 `error.warehouse.unassigned`.
 *
 * ## Why data-driven defaults
 *
 * The mapping lives in a lookup table, not in code, so adding a role's default
 * bodega is a data change. The two "all" roles (ADMINISTRADOR/PERSONAL_ADMINISTRATIVO) and
 * the two "none" roles (MEDICO/MEDICO_RESIDENTE) are represented by their absence
 * from the table plus the role checks below, rather than by enumerating every
 * warehouse.
 */
@Service
@Suppress("ReturnCount")
class WarehouseScopeService(
    private val warehouseRepository: WarehouseRepository,
    private val userWarehouseRepository: UserWarehouseRepository,
    private val roleDefaultWarehouseRepository: RoleDefaultWarehouseRepository,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun resolveDispensingWarehouse(user: CustomUserDetails): Warehouse {
        val mapped = defaultWarehousesForRoles(user.getRoleCodes())
        if (mapped.isNotEmpty()) {
            return mapped.firstOrNull { it.code == WarehouseCodes.NURSING } ?: mapped.first()
        }

        if (user.getRoleCodes().any { it in ALL_OR_FALLBACK_ROLES }) {
            return warehouseRepository.findByCode(WarehouseCodes.NURSING)
                ?: throw UnprocessableEntityException(messageService.errorWarehouseUnassigned())
        }

        val assigned = assignedWarehouses(user.id)
        if (assigned.size == 1) return assigned.first()

        throw UnprocessableEntityException(messageService.errorWarehouseUnassigned())
    }

    /** Source-warehouse scope for transfers and charges (AC-8). */
    @Transactional(readOnly = true)
    fun assertCanUseAsSource(user: CustomUserDetails, warehouse: Warehouse) {
        if (user.getRoleCodes().any { it in ALL_WAREHOUSE_ROLES }) return
        val ownedIds = ownedWarehouseIds(user)
        if (warehouse.id !in ownedIds) {
            throw ForbiddenException(messageService.errorWarehouseTransferSourceDenied())
        }
    }

    /** Stock-view / read scope for a single warehouse (AC-13). */
    @Transactional(readOnly = true)
    fun assertCanView(user: CustomUserDetails, warehouse: Warehouse) {
        if (canViewAll(user)) return
        if (warehouse.id !in ownedWarehouseIds(user)) {
            throw ForbiddenException(messageService.errorWarehouseViewDenied())
        }
    }

    /**
     * Stock-view / read scope by warehouse id (AC-13). Used by warehouse-scoped
     * read facets — the expiry report and the FEFO preview — so a warehouse-scoped
     * caller cannot inspect another bodega's stock by passing its id.
     */
    @Transactional(readOnly = true)
    fun assertCanView(user: CustomUserDetails, warehouseId: Long) {
        val warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow { ResourceNotFoundException(messageService.errorWarehouseNotFound(warehouseId)) }
        assertCanView(user, warehouse)
    }

    /** Warehouses the caller may see in the `GET /warehouses` list. */
    @Transactional(readOnly = true)
    fun visibleWarehouses(user: CustomUserDetails): List<Warehouse> {
        if (canViewAll(user)) return warehouseRepository.findAllByOrderByName()
        val ownedIds = ownedWarehouseIds(user)
        return warehouseRepository.findAllByOrderByName().filter { it.id in ownedIds }
    }

    private fun canViewAll(user: CustomUserDetails): Boolean = user.getRoleCodes().any { it in READ_ALL_ROLES }

    private fun defaultWarehousesForRoles(roleCodes: Set<String>): List<Warehouse> =
        roleDefaultWarehouseRepository.findByRoleCodes(roleCodes).map { it.warehouse }.distinct()

    private fun assignedWarehouses(userId: Long): List<Warehouse> =
        userWarehouseRepository.findByUserId(userId).map { it.warehouse }

    private fun ownedWarehouseIds(user: CustomUserDetails): Set<Long?> =
        (defaultWarehousesForRoles(user.getRoleCodes()) + assignedWarehouses(user.id))
            .map { it.id }
            .toSet()

    private companion object {
        /** Roles that may use any warehouse as a transfer/charge source. */
        val ALL_WAREHOUSE_ROLES = setOf("ADMINISTRADOR", "PERSONAL_ADMINISTRATIVO")

        /** Roles that fall back to ENFERMERIA when no default mapping exists. */
        val ALL_OR_FALLBACK_ROLES = setOf("ADMINISTRADOR", "PERSONAL_ADMINISTRATIVO", "MEDICO", "MEDICO_RESIDENTE")

        /** Roles that can view every warehouse in the stock view. */
        val READ_ALL_ROLES = setOf("ADMINISTRADOR", "PERSONAL_ADMINISTRATIVO", "MEDICO", "MEDICO_RESIDENTE")
    }
}
