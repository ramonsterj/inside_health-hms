package com.insidehealthgt.hms.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * Assigns a MAINTENANCE user to a warehouse they operate. The service layer
 * scopes their transfer-source and stock-view access to these rows. Removing the
 * MAINTENANCE role does NOT delete the rows — they go dormant and re-activate if
 * the role is re-added (FR-10).
 */
@Entity
@Table(name = "user_warehouses")
@SQLRestriction("deleted_at IS NULL")
class UserWarehouse(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse,

) : BaseEntity()
