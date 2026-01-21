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
}
