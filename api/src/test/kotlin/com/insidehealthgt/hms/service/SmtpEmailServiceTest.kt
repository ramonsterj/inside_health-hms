package com.insidehealthgt.hms.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class SmtpEmailServiceTest {

    private lateinit var smtpEmailService: SmtpEmailService

    @BeforeEach
    fun setUp() {
        smtpEmailService = SmtpEmailService()
    }

    @Test
    fun `sendPasswordResetEmail should throw UnsupportedOperationException`() {
        val exception = assertThrows<UnsupportedOperationException> {
            smtpEmailService.sendPasswordResetEmail("test@example.com", "token")
        }

        assertTrue(exception.message!!.contains("not configured"))
    }

    @Test
    fun `exception message should mention SMTP configuration`() {
        val exception = assertThrows<UnsupportedOperationException> {
            smtpEmailService.sendPasswordResetEmail("test@example.com", "token")
        }

        assertTrue(exception.message!!.contains("SMTP"))
    }
}
