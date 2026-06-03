package com.insidehealthgt.hms.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * Membership of a canonical [LabTest] in a [LabPanel].
 */
@Entity
@Table(name = "lab_panel_items")
@SQLRestriction("deleted_at IS NULL")
class LabPanelItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id", nullable = false)
    var panel: LabPanel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    var labTest: LabTest,

) : BaseEntity()
