package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "emergency_contacts")
@SQLRestriction("deleted_at IS NULL")
class EmergencyContact(

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 100)
    var relationship: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient? = null,

) : BaseEntity()
