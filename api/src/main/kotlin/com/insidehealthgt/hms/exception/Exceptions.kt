package com.insidehealthgt.hms.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(message: String = "Unauthorized") : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(message: String = "Access denied") : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidCredentialsException(message: String = "Invalid email or password") : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class AccountDisabledException(message: String = "Account is not active") : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicatePatientException(message: String, val potentialDuplicates: List<DuplicatePatientInfo>) :
    RuntimeException(message)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class FileStorageException(message: String) : RuntimeException(message)

data class DuplicatePatientInfo(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val idDocumentNumber: String?,
)
