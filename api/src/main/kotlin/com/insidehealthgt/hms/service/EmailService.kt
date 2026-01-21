package com.insidehealthgt.hms.service

interface EmailService {
    fun sendPasswordResetEmail(email: String, resetToken: String)
}
