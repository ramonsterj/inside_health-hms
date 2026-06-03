package com.insidehealthgt.hms.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * A named preset of canonical tests (e.g. "Laboratorios de ingreso"). Items are defined in
 * canonical [LabTest]s; resolving a panel against a provider yields that provider's
 * [LabProviderTest]s for the offered tests plus the unmatched canonical tests.
 */
@Entity
@Table(name = "lab_panels")
@SQLRestriction("deleted_at IS NULL")
class LabPanel(

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @OneToMany(mappedBy = "panel", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var items: MutableList<LabPanelItem> = mutableListOf(),

) : BaseEntity()
