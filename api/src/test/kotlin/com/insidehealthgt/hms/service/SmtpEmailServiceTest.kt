package com.insidehealthgt.hms.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class SmtpEmailServiceTest {

    private lateinit var smtpEmailService: SmtpEmailService

    @BeforeEach
    fun setUp() {
        smtpEmailService = SmtpEmailService()
    }

    @Test
    fun `sendPasswordResetEmail should throw UnsupportedOperationException mentioning SMTP`() {
        val exception = assertThrows<UnsupportedOperationException> {
            smtpEmailService.sendPasswordResetEmail("test@example.com", "token")
        }

        val message = exception.message
        assertNotNull(message)
        assertContains(message, "SMTP")
        assertContains(message, "not configured")
    }
}
