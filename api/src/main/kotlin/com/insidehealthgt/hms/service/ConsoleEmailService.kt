package com.insidehealthgt.hms.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["email.provider"],
    havingValue = "console",
    matchIfMissing = true,
)
class ConsoleEmailService : EmailService {

    private val logger = LoggerFactory.getLogger(ConsoleEmailService::class.java)

    companion object {
        private const val RESET_URL_TEMPLATE = "http://localhost:5173/reset-password?token=%s"
    }

    override fun sendPasswordResetEmail(email: String, resetToken: String) {
        val resetUrl = String.format(RESET_URL_TEMPLATE, resetToken)

        logger.info("======================================")
        logger.info("PASSWORD RESET EMAIL")
        logger.info("To: $email")
        logger.info("Reset URL: $resetUrl")
        logger.info("Token: $resetToken")
        logger.info("======================================")
    }
}
