package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "medication_details")
@SQLRestriction("deleted_at IS NULL")
class MedicationDetails(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    var item: InventoryItem,

    @Column(name = "generic_name", nullable = false, length = 150)
    var genericName: String,

    @Column(name = "commercial_name", length = 150)
    var commercialName: String? = null,

    @Column(length = 50)
    var strength: String? = null,

    @Column(name = "dosage_form", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var dosageForm: DosageForm,

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    var route: AdministrationRoute? = null,

    @Column(nullable = false)
    var controlled: Boolean = false,

    @Column(name = "atc_code", length = 10)
    var atcCode: String? = null,

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var section: MedicationSection,

    @Column(name = "review_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var reviewStatus: MedicationReviewStatus = MedicationReviewStatus.CONFIRMED,

    @Column(name = "review_notes", length = 500)
    var reviewNotes: String? = null,

) : BaseEntity()
