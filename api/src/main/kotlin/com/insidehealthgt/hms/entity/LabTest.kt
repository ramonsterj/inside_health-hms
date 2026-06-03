package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * A canonical laboratory test concept (e.g. "Hematología completa"). Providers attach
 * their own name and pricing to a canonical test via [LabProviderTest]; panels are
 * composed from canonical tests via [LabPanelItem].
 */
@Entity
@Table(name = "lab_tests")
@SQLRestriction("deleted_at IS NULL")
class LabTest(

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()
