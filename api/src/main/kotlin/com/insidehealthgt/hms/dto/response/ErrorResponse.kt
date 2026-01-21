package com.insidehealthgt.hms.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetails,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)

data class ErrorDetails(val code: String, val message: String, val details: Map<String, List<String>>? = null)
