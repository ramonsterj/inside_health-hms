package com.insidehealthgt.hms.service

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
@Suppress("TooManyFunctions")
class MessageService(private val messageSource: MessageSource) {

    /**
     * Get a localized message using the current locale from LocaleContextHolder.
     */
    fun getMessage(code: String, vararg args: Any): String = getMessage(code, LocaleContextHolder.getLocale(), *args)

    /**
     * Get a localized message for a specific locale.
     */
    fun getMessage(code: String, locale: Locale, vararg args: Any): String =
        messageSource.getMessage(code, args, code, locale) ?: code

    /**
     * Get a localized message with a default fallback.
     */
    fun getMessageOrDefault(code: String, defaultMessage: String, vararg args: Any): String =
        messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale())
            ?: defaultMessage

    // === Authentication Messages ===

    fun authLoginSuccess() = getMessage("auth.login.success")
    fun authLoginFailed() = getMessage("auth.login.failed")
    fun authLogoutSuccess() = getMessage("auth.logout.success")
    fun authRegisterSuccess() = getMessage("auth.register.success")
    fun authTokenRefreshed() = getMessage("auth.token.refreshed")
    fun authPasswordResetInitiated() = getMessage("auth.password.reset.initiated")
    fun authPasswordResetSuccess() = getMessage("auth.password.reset.success")

    // === User Management Messages ===

    fun userCreated() = getMessage("user.created")
    fun userUpdated() = getMessage("user.updated")
    fun userDeleted() = getMessage("user.deleted")
    fun userRestored() = getMessage("user.restored")
    fun userPasswordChanged() = getMessage("user.password.changed")
    fun userPasswordReset() = getMessage("user.password.reset")
    fun userProfileUpdated() = getMessage("user.profile.updated")
    fun userLocaleUpdated() = getMessage("user.locale.updated")
    fun userRolesAssigned() = getMessage("user.roles.assigned")

    // === Username Availability ===

    fun usernameAvailable() = getMessage("username.available")
    fun usernameTaken() = getMessage("username.taken")

    // === Role Management Messages ===

    fun roleCreated() = getMessage("role.created")
    fun roleUpdated() = getMessage("role.updated")
    fun roleDeleted() = getMessage("role.deleted")
    fun rolePermissionsAssigned() = getMessage("role.permissions.assigned")

    // === Error Messages ===

    fun errorNotFound() = getMessage("error.not.found")
    fun errorBadRequest() = getMessage("error.bad.request")
    fun errorUnauthorized() = getMessage("error.unauthorized")
    fun errorForbidden() = getMessage("error.forbidden")
    fun errorConflict() = getMessage("error.conflict")
    fun errorValidation() = getMessage("error.validation")
    fun errorInternal() = getMessage("error.internal")
    fun errorFileTooLarge() = getMessage("error.file.too.large")
    fun errorInvalidToken() = getMessage("error.invalid.token")
    fun errorInvalidCredentials() = getMessage("error.invalid.credentials")
    fun errorAccountDisabled() = getMessage("error.account.disabled")
    fun errorAuthenticationFailed() = getMessage("error.authentication.failed")

    // === Parameterized Error Messages ===

    fun errorUserNotFound(id: Long) = getMessage("error.user.not.found", id)
    fun errorUserEmailNotFound(email: String) = getMessage("error.user.email.not.found", email)
    fun errorUserEmailExists(email: String) = getMessage("error.user.email.exists", email)
    fun errorUserUsernameExists(username: String) = getMessage("error.user.username.exists", username)
    fun errorUserPasswordIncorrect() = getMessage("error.user.password.incorrect")
    fun errorUserCannotDeleteSelf() = getMessage("error.user.cannot.delete.self")

    fun errorRoleNotFound(id: Long) = getMessage("error.role.not.found", id)
    fun errorRoleCodeNotFound(code: String) = getMessage("error.role.code.not.found", code)
    fun errorRoleCodeExists(code: String) = getMessage("error.role.code.exists", code)
    fun errorRoleSystemCannotDelete(code: String) = getMessage("error.role.system.cannot.delete", code)
    fun errorRoleInvalidCodes(codes: String) = getMessage("error.role.invalid.codes", codes)

    fun errorPermissionNotFound(id: Long) = getMessage("error.permission.not.found", id)
    fun errorPermissionInvalidIds(ids: String) = getMessage("error.permission.invalid.ids", ids)

    fun errorLocaleUnsupported(locale: String, supported: String) =
        getMessage("error.locale.unsupported", locale, supported)

    // === Common Messages ===

    fun commonSuccess() = getMessage("common.success")
    fun commonCreated() = getMessage("common.created")
    fun commonUpdated() = getMessage("common.updated")
    fun commonDeleted() = getMessage("common.deleted")

    // === Admission Messages ===

    fun admissionDeleted() = getMessage("admission.deleted")
    fun admissionDocumentDeleted() = getMessage("admission.document.deleted")
    fun medicalOrderDocumentDeleted() = getMessage("medical.order.document.deleted")

    // === Admission Error Messages ===

    fun errorAdmissionNotFound(id: Long) = getMessage("error.admission.not.found", id)
    fun errorAdmissionPatientNotFound(id: Long) = getMessage("error.admission.patient.not.found", id)
    fun errorAdmissionTriageNotFound(id: Long) = getMessage("error.admission.triage.not.found", id)
    fun errorAdmissionRoomNotFound(id: Long) = getMessage("error.admission.room.not.found", id)
    fun errorAdmissionUserNotFound(id: Long) = getMessage("error.admission.user.not.found", id)
    fun errorAdmissionPatientActive() = getMessage("error.admission.patient.active")
    fun errorAdmissionTriageRequired(type: String) = getMessage("error.admission.triage.required", type)
    fun errorAdmissionRoomRequired(type: String) = getMessage("error.admission.room.required", type)
    fun errorAdmissionPhysicianRole() = getMessage("error.admission.physician.role")
    fun errorAdmissionRoomFull(roomNumber: String) = getMessage("error.admission.room.full", roomNumber)
    fun errorAdmissionUpdateDischarged() = getMessage("error.admission.update.discharged")
    fun errorAdmissionAlreadyDischarged() = getMessage("error.admission.already.discharged")
    fun errorAdmissionConsultingPhysicianRole() = getMessage("error.admission.consulting.physician.role")
    fun errorAdmissionConsultingPhysicianIsTreating() = getMessage("error.admission.consulting.physician.is.treating")
    fun errorAdmissionConsultingPhysicianAlreadyAssigned() =
        getMessage("error.admission.consulting.physician.already.assigned")
    fun errorAdmissionConsultingPhysicianNotFound(id: Long, admissionId: Long) =
        getMessage("error.admission.consulting.physician.not.found", id, admissionId)
    fun errorAdmissionConsentFileEmpty() = getMessage("error.admission.consent.file.empty")
    fun errorAdmissionConsentFileSize() = getMessage("error.admission.consent.file.size")
    fun errorAdmissionConsentFileType() = getMessage("error.admission.consent.file.type")
    fun errorAdmissionConsentNotFound(admissionId: Long) = getMessage("error.admission.consent.not.found", admissionId)

    // === Patient Error Messages ===

    fun errorPatientNotFound(id: Long) = getMessage("error.patient.not.found", id)
    fun errorPatientDuplicate() = getMessage("error.patient.duplicate")
    fun errorPatientIdDocumentNotFound(patientId: Long) = getMessage("error.patient.id.document.not.found", patientId)
    fun errorPatientFileEmpty() = getMessage("error.patient.file.empty")
    fun errorPatientFileSize() = getMessage("error.patient.file.size")
    fun errorPatientFileType() = getMessage("error.patient.file.type")
    fun errorPatientAccessDenied() = getMessage("error.patient.access.denied")
    fun errorPatientDateOfBirthTooOld(maxYears: Int) = getMessage("error.patient.dateOfBirth.tooOld", maxYears)

    // === Vital Sign Error Messages ===

    fun errorVitalSignNotFound(id: Long, admissionId: Long) = getMessage("error.vitalSign.not.found", id, admissionId)
    fun errorVitalSignRecordedAtBeforeAdmission() = getMessage("error.vitalSign.recordedAt.before.admission")
    fun errorVitalSignRecordedAtFuture() = getMessage("error.vitalSign.recordedAt.future")
    fun errorVitalSignSystolicGreaterThanDiastolic() = getMessage("error.vitalSign.systolicGreaterThanDiastolic")

    // === Nursing Note Error Messages ===

    fun errorNursingNoteNotFound(id: Long, admissionId: Long) =
        getMessage("error.nursingNote.not.found", id, admissionId)

    // === Psychotherapy Error Messages ===

    fun errorPsychotherapyActivityNotFound(id: Long, admissionId: Long) =
        getMessage("error.psychotherapy.activity.not.found", id, admissionId)
    fun errorPsychotherapyActivityOnlyPsychologist() = getMessage("error.psychotherapy.activity.only.psychologist")
    fun errorPsychotherapyActivityOnlyHospitalized() = getMessage("error.psychotherapy.activity.only.hospitalized")
    fun errorPsychotherapyCategoryNotFound(id: Long) = getMessage("error.psychotherapy.category.not.found", id)
    fun errorPsychotherapyCategoryDuplicateName(name: String) =
        getMessage("error.psychotherapy.category.duplicate.name", name)
    fun errorPsychotherapyCategoryInUse() = getMessage("error.psychotherapy.category.in.use")

    // === Clinical History Error Messages ===

    fun errorClinicalHistoryNotFound(admissionId: Long) = getMessage("error.clinicalHistory.not.found", admissionId)
    fun errorClinicalHistoryAlreadyExists() = getMessage("error.clinicalHistory.already.exists")

    // === Document Type Error Messages ===

    fun errorDocumentTypeNotFound(id: Long) = getMessage("error.documentType.not.found", id)
    fun errorDocumentTypeCodeNotFound(code: String) = getMessage("error.documentType.code.not.found", code)
    fun errorDocumentTypeCodeExists(code: String) = getMessage("error.documentType.code.exists", code)
    fun errorDocumentTypeHasDocuments() = getMessage("error.documentType.has.documents")

    // === Inventory Error Messages ===

    fun errorInventoryItemNotFound(id: Long) = getMessage("error.inventory.item.not.found", id)
    fun errorInventoryCategoryNotFound(id: Long) = getMessage("error.inventory.category.not.found", id)
    fun errorInventoryInsufficientStock(currentQuantity: Int, requested: Int) =
        getMessage("error.inventory.insufficient.stock", currentQuantity, requested)
    fun errorInventoryTimeUnitRequired() = getMessage("error.inventory.time.unit.required")
    fun errorInventoryTimeIntervalRequired() = getMessage("error.inventory.time.interval.required")
    fun errorInventoryAdmissionNotFound(id: Long) = getMessage("error.inventory.admission.not.found", id)

    // === Billing Error Messages ===

    fun errorBillingChargeTypeNotAllowed(chargeType: String) =
        getMessage("error.billing.charge.type.not.allowed", chargeType)
    fun errorBillingAdmissionNotActive() = getMessage("error.billing.admission.not.active")

    // === Medication Administration Error Messages ===

    fun errorMedicationOrderNotFound(orderId: Long, admissionId: Long) =
        getMessage("error.medication.order.not.found", orderId, admissionId)
    fun errorMedicationOnlyMedicamentos() = getMessage("error.medication.only.medicamentos")
    fun errorMedicationAdmissionDischarged() = getMessage("error.medication.admission.discharged")
    fun errorMedicationOrderDiscontinued() = getMessage("error.medication.order.discontinued")
    fun errorMedicationOrderNoInventory() = getMessage("error.medication.order.no.inventory")

    // === Common Error Messages ===

    fun errorNotAuthenticated() = getMessage("error.not.authenticated")
    fun errorAdmissionDischargedRecords() = getMessage("error.admission.discharged.records")

    // === Auth Error Messages ===

    fun errorAuthAccountStatus(status: String) = getMessage("error.auth.account.status", status)
    fun errorAuthAccountStatusInactive() = getMessage("error.auth.account.status.inactive")
    fun errorAuthAccountStatusSuspended() = getMessage("error.auth.account.status.suspended")
    fun errorAuthAccountStatusDeleted() = getMessage("error.auth.account.status.deleted")
    fun errorAuthAccountStatusActive() = getMessage("error.auth.account.status.active")

    // === File Storage Error Messages ===

    fun errorFileStorageInit(path: String) = getMessage("error.file.storage.init", path)
    fun errorFileStorageNotWritable(path: String) = getMessage("error.file.storage.not.writable", path)
    fun errorFileStorageRead() = getMessage("error.file.storage.read")
    fun errorFileStorageWrite() = getMessage("error.file.storage.write")
    fun errorFileStorageInvalidPath() = getMessage("error.file.storage.invalid.path")
    fun errorFileStorageInvalidFilename() = getMessage("error.file.storage.invalid.filename")
    fun errorFileStorageNotFound() = getMessage("error.file.storage.not.found")

    // === Medical Order Document Error Messages ===

    fun errorMedicalOrderDocumentFileEmpty() = getMessage("error.medical.order.document.file.empty")
    fun errorMedicalOrderDocumentFileSize() = getMessage("error.medical.order.document.file.size")
    fun errorMedicalOrderDocumentFileType() = getMessage("error.medical.order.document.file.type")
    fun errorMedicalOrderDocumentInvalidStatus() = getMessage("error.medical.order.document.invalid.status")
    fun errorMedicalOrderDocumentInvalidCategory() = getMessage("error.medical.order.document.invalid.category")

    // === Duplicate Patient Error Messages ===

    fun errorPatientDuplicateFound() = getMessage("error.patient.duplicate.found")
}
