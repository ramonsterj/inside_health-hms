package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "progress_notes")
@SQLRestriction("deleted_at IS NULL")
class ProgressNote(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(name = "subjective_data", columnDefinition = "TEXT")
    var subjectiveData: String? = null,

    @Column(name = "objective_data", columnDefinition = "TEXT")
    var objectiveData: String? = null,

    @Column(columnDefinition = "TEXT")
    var analysis: String? = null,

    @Column(name = "action_plans", columnDefinition = "TEXT")
    var actionPlans: String? = null,

) : BaseEntity()
