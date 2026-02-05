package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "clinical_histories")
@SQLRestriction("deleted_at IS NULL")
class ClinicalHistory(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false, unique = true)
    var admission: Admission,

    @Column(name = "reason_for_admission", columnDefinition = "TEXT")
    var reasonForAdmission: String? = null,

    @Column(name = "history_of_present_illness", columnDefinition = "TEXT")
    var historyOfPresentIllness: String? = null,

    @Column(name = "psychiatric_history", columnDefinition = "TEXT")
    var psychiatricHistory: String? = null,

    @Column(name = "medical_history", columnDefinition = "TEXT")
    var medicalHistory: String? = null,

    @Column(name = "family_history", columnDefinition = "TEXT")
    var familyHistory: String? = null,

    @Column(name = "personal_history", columnDefinition = "TEXT")
    var personalHistory: String? = null,

    @Column(name = "substance_use_history", columnDefinition = "TEXT")
    var substanceUseHistory: String? = null,

    @Column(name = "legal_history", columnDefinition = "TEXT")
    var legalHistory: String? = null,

    @Column(name = "social_history", columnDefinition = "TEXT")
    var socialHistory: String? = null,

    @Column(name = "developmental_history", columnDefinition = "TEXT")
    var developmentalHistory: String? = null,

    @Column(name = "educational_occupational_history", columnDefinition = "TEXT")
    var educationalOccupationalHistory: String? = null,

    @Column(name = "sexual_history", columnDefinition = "TEXT")
    var sexualHistory: String? = null,

    @Column(name = "religious_spiritual_history", columnDefinition = "TEXT")
    var religiousSpiritualHistory: String? = null,

    @Column(name = "mental_status_exam", columnDefinition = "TEXT")
    var mentalStatusExam: String? = null,

    @Column(name = "physical_exam", columnDefinition = "TEXT")
    var physicalExam: String? = null,

    @Column(name = "diagnostic_impression", columnDefinition = "TEXT")
    var diagnosticImpression: String? = null,

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    var treatmentPlan: String? = null,

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    var riskAssessment: String? = null,

    @Column(name = "prognosis", columnDefinition = "TEXT")
    var prognosis: String? = null,

    @Column(name = "informed_consent_notes", columnDefinition = "TEXT")
    var informedConsentNotes: String? = null,

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    var additionalNotes: String? = null,

) : BaseEntity()
