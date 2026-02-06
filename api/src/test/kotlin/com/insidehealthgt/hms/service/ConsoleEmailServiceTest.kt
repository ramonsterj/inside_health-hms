package com.insidehealthgt.hms.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class ConsoleEmailServiceTest {

    private lateinit var consoleEmailService: ConsoleEmailService
    private lateinit var listAppender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setUp() {
        consoleEmailService = ConsoleEmailService()

        listAppender = ListAppender()
        listAppender.start()
        val logger = LoggerFactory.getLogger(ConsoleEmailService::class.java) as Logger
        logger.addAppender(listAppender)
    }

    @AfterEach
    fun tearDown() {
        val logger = LoggerFactory.getLogger(ConsoleEmailService::class.java) as Logger
        logger.detachAppender(listAppender)
        listAppender.stop()
    }

    @Test
    fun `sendPasswordResetEmail should log email and token`() {
        consoleEmailService.sendPasswordResetEmail("test@example.com", "test-token-123")

        val messages = listAppender.list.map { it.formattedMessage }
        assertTrue(messages.any { it.contains("test@example.com") })
        assertTrue(messages.any { it.contains("test-token-123") })
    }

    @Test
    fun `sendPasswordResetEmail should log reset URL with token`() {
        consoleEmailService.sendPasswordResetEmail("test@example.com", "token-abc-123")

        val messages = listAppender.list.map { it.formattedMessage }
        assertTrue(messages.any { it.contains("token-abc-123") && it.contains("http") })
    }

    @Test
    fun `sendPasswordResetEmail should handle special characters in email`() {
        consoleEmailService.sendPasswordResetEmail("test+special@example.com", "token-abc-123")

        val messages = listAppender.list.map { it.formattedMessage }
        assertTrue(messages.any { it.contains("test+special@example.com") })
    }
}
