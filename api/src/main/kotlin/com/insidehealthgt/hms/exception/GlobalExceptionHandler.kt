package com.insidehealthgt.hms.exception

import com.insidehealthgt.hms.dto.response.DuplicatePatientData
import com.insidehealthgt.hms.dto.response.DuplicatePatientResponse
import com.insidehealthgt.hms.dto.response.ErrorDetails
import com.insidehealthgt.hms.dto.response.ErrorResponse
import com.insidehealthgt.hms.service.MessageService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Suppress("TooManyFunctions")
class GlobalExceptionHandler(private val messageService: MessageService) {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        logger.debug("Resource not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "NOT_FOUND",
                        message = ex.message ?: messageService.errorNotFound(),
                    ),
                ),
            )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<ErrorResponse> {
        logger.debug("Bad request: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "BAD_REQUEST",
                        message = ex.message ?: messageService.errorBadRequest(),
                    ),
                ),
            )
    }

    @ExceptionHandler(UnauthorizedException::class, InvalidCredentialsException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.debug("Unauthorized: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "UNAUTHORIZED",
                        message = ex.message ?: messageService.errorUnauthorized(),
                    ),
                ),
            )
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException): ResponseEntity<ErrorResponse> {
        logger.debug("Invalid token: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "INVALID_TOKEN",
                        message = ex.message ?: messageService.errorInvalidToken(),
                    ),
                ),
            )
    }

    @ExceptionHandler(ForbiddenException::class, AccessDeniedException::class)
    fun handleForbidden(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.debug("Access denied: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "FORBIDDEN",
                        message = ex.message ?: messageService.errorForbidden(),
                    ),
                ),
            )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<ErrorResponse> {
        logger.debug("Conflict: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "CONFLICT",
                        message = ex.message ?: messageService.errorConflict(),
                    ),
                ),
            )
    }

    @ExceptionHandler(AccountDisabledException::class)
    fun handleAccountDisabled(ex: AccountDisabledException): ResponseEntity<ErrorResponse> {
        logger.debug("Account disabled: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "ACCOUNT_DISABLED",
                        message = ex.message ?: messageService.errorAccountDisabled(),
                    ),
                ),
            )
    }

    @ExceptionHandler(DuplicatePatientException::class)
    fun handleDuplicatePatient(ex: DuplicatePatientException): ResponseEntity<DuplicatePatientResponse> {
        logger.debug("Duplicate patient detected: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                DuplicatePatientResponse(
                    success = false,
                    message = ex.message ?: "Potential duplicate patient found",
                    data = DuplicatePatientData(potentialDuplicates = ex.potentialDuplicates),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { (_, errors) -> errors.mapNotNull { it.defaultMessage } }

        logger.debug("Validation failed: {}", details)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "VALIDATION_ERROR",
                        message = messageService.errorValidation(),
                        details = details,
                    ),
                ),
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.debug("Invalid request body: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "INVALID_REQUEST_BODY",
                        message = messageService.errorBadRequest(),
                    ),
                ),
            )
    }

    @ExceptionHandler(AuthenticationException::class, BadCredentialsException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        logger.debug("Authentication failed: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "AUTHENTICATION_FAILED",
                        message = messageService.errorAuthenticationFailed(),
                    ),
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    error = ErrorDetails(
                        code = "INTERNAL_ERROR",
                        message = messageService.errorInternal(),
                    ),
                ),
            )
    }
}
