package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.service.MessageService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider(private val messageService: MessageService) {

    fun currentUserDetails(): CustomUserDetails? {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth != null && auth.isAuthenticated) auth.principal as? CustomUserDetails else null
    }

    fun currentUserDetailsOrThrow(): CustomUserDetails = currentUserDetails()
        ?: throw UnauthorizedException(messageService.errorNotAuthenticated())

    fun currentUserId(): Long? = currentUserDetails()?.id

    fun currentUserIdOrThrow(): Long = currentUserDetailsOrThrow().id

    /**
     * Belt-and-suspenders guard for the three nursing actions auxiliary nurses may not perform
     * (administer medication, mark order in progress, upload result document). Throws 403
     * [ForbiddenException] when the caller's only nursing-or-better role is AUXILIAR_ENFERMERIA — even
     * if a custom role granted the underlying permission. See docs/features/nursing-roles-split.md.
     */
    fun requireNotAuxiliaryNurseOnly() {
        if (currentUserDetailsOrThrow().isAuxiliaryNurseOnly()) {
            throw ForbiddenException(messageService.errorNursingAuxiliaryDenied())
        }
    }
}
