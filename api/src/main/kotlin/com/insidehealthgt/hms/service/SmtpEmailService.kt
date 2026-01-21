package com.insidehealthgt.hms.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["email.provider"], havingValue = "smtp")
class SmtpEmailService : EmailService {

    override fun sendPasswordResetEmail(email: String, resetToken: String) = throw UnsupportedOperationException(
        "SMTP email service is not configured. " +
            "Please configure SMTP settings or use console provider for development.",
    )
}
