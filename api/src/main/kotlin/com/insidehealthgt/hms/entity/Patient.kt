package com.insidehealthgt.hms.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "patients")
@SQLRestriction("deleted_at IS NULL")
class Patient(

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Column(nullable = false)
    var age: Int,

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var sex: Sex,

    @Column(nullable = false, length = 50)
    var gender: String,

    @Column(name = "marital_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var maritalStatus: MaritalStatus,

    @Column(nullable = false, length = 100)
    var religion: String,

    @Column(name = "education_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var educationLevel: EducationLevel,

    @Column(nullable = false, length = 100)
    var occupation: String,

    @Column(nullable = false, length = 500)
    var address: String,

    @Column(nullable = false, length = 255)
    var email: String,

    @Column(name = "id_document_number", length = 50)
    var idDocumentNumber: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var emergencyContacts: MutableList<EmergencyContact> = mutableListOf(),

    @OneToOne(mappedBy = "patient", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var idDocument: PatientIdDocument? = null,

) : BaseEntity() {

    fun hasIdDocument(): Boolean = idDocument != null

    fun addEmergencyContact(contact: EmergencyContact) {
        emergencyContacts.add(contact)
        contact.patient = this
    }

    fun removeEmergencyContact(contact: EmergencyContact) {
        emergencyContacts.remove(contact)
        contact.patient = null
    }
}

enum class Sex {
    MALE,
    FEMALE,
}

enum class MaritalStatus {
    SINGLE,
    MARRIED,
    DIVORCED,
    WIDOWED,
    SEPARATED,
    OTHER,
}

enum class EducationLevel {
    NONE,
    PRIMARY,
    SECONDARY,
    TECHNICAL,
    UNIVERSITY,
    POSTGRADUATE,
}
