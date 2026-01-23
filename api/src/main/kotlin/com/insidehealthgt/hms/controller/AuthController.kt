package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.ForgotPasswordRequest
import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.dto.request.RefreshTokenRequest
import com.insidehealthgt.hms.dto.request.ResetPasswordRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.dto.response.UsernameAvailabilityResponse
import com.insidehealthgt.hms.service.AuthService
import com.insidehealthgt.hms.service.MessageService
import com.insidehealthgt.hms.service.PasswordResetService
import com.insidehealthgt.hms.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val passwordResetService: PasswordResetService,
    private val userService: UserService,
    private val messageService: MessageService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(authResponse, messageService.authLoginSuccess()))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.refresh(request)
        return ResponseEntity.ok(ApiResponse.success(authResponse, messageService.authTokenRefreshed()))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<ApiResponse<Unit>> {
        authService.logout()
        return ResponseEntity.ok(ApiResponse.success(messageService.authLogoutSuccess()))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<ApiResponse<Unit>> {
        passwordResetService.initiatePasswordReset(request.email)
        return ResponseEntity.ok(ApiResponse.success(messageService.authPasswordResetInitiated()))
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse<Unit>> {
        passwordResetService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(ApiResponse.success(messageService.authPasswordResetSuccess()))
    }

    @GetMapping("/check-username")
    fun checkUsername(
        @RequestParam
        @Size(min = 3, max = 50, message = "{validation.username.size}")
        username: String,
    ): ResponseEntity<ApiResponse<UsernameAvailabilityResponse>> {
        val available = userService.isUsernameAvailable(username)
        val response = UsernameAvailabilityResponse(
            available = available,
            message = if (available) messageService.usernameAvailable() else messageService.usernameTaken(),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
