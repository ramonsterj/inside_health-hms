package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "inventory_movements")
@SQLRestriction("deleted_at IS NULL")
class InventoryMovement(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: InventoryItem,

    @Column(name = "movement_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var movementType: MovementType,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "previous_quantity", nullable = false)
    var previousQuantity: Int,

    @Column(name = "new_quantity", nullable = false)
    var newQuantity: Int,

    @Column(length = 500)
    var notes: String? = null,

) : BaseEntity()
