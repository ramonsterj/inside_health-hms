package com.insidehealthgt.hms.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * Data-driven role -> default-warehouse mapping (FR-9). Adding a role's default
 * bodega is a data change, not a code change. ADMIN/ADMINISTRATIVE_STAFF (all)
 * and DOCTOR/RESIDENT_DOCTOR (none) are represented by absence + a role check in
 * [com.insidehealthgt.hms.service.WarehouseScopeService].
 */
@Entity
@Table(name = "role_default_warehouses")
@SQLRestriction("deleted_at IS NULL")
class RoleDefaultWarehouse(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    var role: Role,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse,

) : BaseEntity()
