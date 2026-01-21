package com.insidehealthgt.hms.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConsoleEmailServiceTest {

    private lateinit var consoleEmailService: ConsoleEmailService

    @BeforeEach
    fun setUp() {
        consoleEmailService = ConsoleEmailService()
    }

    @Test
    fun `sendPasswordResetEmail should not throw exception`() {
        consoleEmailService.sendPasswordResetEmail("test@example.com", "test-token-123")
    }

    @Test
    fun `sendPasswordResetEmail should handle special characters in email`() {
        consoleEmailService.sendPasswordResetEmail("test+special@example.com", "token-abc-123")
    }

    @Test
    fun `sendPasswordResetEmail should handle long tokens`() {
        val longToken = "a".repeat(64)
        consoleEmailService.sendPasswordResetEmail("test@example.com", longToken)
    }
}
