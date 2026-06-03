package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * An external laboratory provider (e.g. CLONY, Hospital Herrera Llerandi). Each provider
 * exposes its own named/priced catalog of canonical tests via [LabProviderTest].
 */
@Entity
@Table(name = "lab_providers")
@SQLRestriction("deleted_at IS NULL")
class LabProvider(

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(length = 50)
    var code: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()
