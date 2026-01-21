package com.insidehealthgt.hms.dto.response

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, data = data, message = message)

        fun <T> success(message: String): ApiResponse<T> = ApiResponse(success = true, data = null, message = message)

        fun <T> error(message: String): ApiResponse<T> = ApiResponse(success = false, data = null, message = message)
    }
}
