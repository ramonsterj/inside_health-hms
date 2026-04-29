package com.insidehealthgt.hms.security

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
}
