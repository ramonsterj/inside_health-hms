package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Sex
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreatePatientRequest(
    @field:NotBlank(message = "{validation.patient.firstName.required}")
    @field:Size(max = 100, message = "{validation.patient.firstName.max}")
    val firstName: String,

    @field:NotBlank(message = "{validation.patient.lastName.required}")
    @field:Size(max = 100, message = "{validation.patient.lastName.max}")
    val lastName: String,

    @field:NotNull(message = "{validation.patient.age.required}")
    @field:Min(value = 0, message = "{validation.patient.age.min}")
    @field:Max(value = 150, message = "{validation.patient.age.max}")
    val age: Int,

    @field:NotNull(message = "{validation.patient.sex.required}")
    val sex: Sex,

    @field:NotBlank(message = "{validation.patient.gender.required}")
    @field:Size(max = 50, message = "{validation.patient.gender.max}")
    val gender: String,

    @field:NotNull(message = "{validation.patient.maritalStatus.required}")
    val maritalStatus: MaritalStatus,

    @field:NotBlank(message = "{validation.patient.religion.required}")
    @field:Size(max = 100, message = "{validation.patient.religion.max}")
    val religion: String,

    @field:NotNull(message = "{validation.patient.educationLevel.required}")
    val educationLevel: EducationLevel,

    @field:NotBlank(message = "{validation.patient.occupation.required}")
    @field:Size(max = 100, message = "{validation.patient.occupation.max}")
    val occupation: String,

    @field:NotBlank(message = "{validation.patient.address.required}")
    @field:Size(max = 500, message = "{validation.patient.address.max}")
    val address: String,

    @field:NotBlank(message = "{validation.patient.email.required}")
    @field:Email(message = "{validation.patient.email.invalid}")
    @field:Size(max = 255, message = "{validation.patient.email.max}")
    val email: String,

    @field:Size(max = 50, message = "{validation.patient.idDocumentNumber.max}")
    val idDocumentNumber: String? = null,

    val notes: String? = null,

    @field:NotEmpty(message = "{validation.patient.emergencyContacts.required}")
    @field:Valid
    val emergencyContacts: List<EmergencyContactRequest>,
)
