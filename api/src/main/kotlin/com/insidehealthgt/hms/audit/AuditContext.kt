package com.insidehealthgt.hms.audit

/**
 * Holds audit-related context for the current request thread.
 * This is populated by AuditContextFilter and used by AuditEntityListener.
 */
data class AuditContextData(val ipAddress: String?, val userAgent: String? = null)

object AuditContext {
    private val contextHolder = ThreadLocal<AuditContextData?>()

    fun set(data: AuditContextData) {
        contextHolder.set(data)
    }

    fun get(): AuditContextData? = contextHolder.get()

    fun getIpAddress(): String? = contextHolder.get()?.ipAddress

    fun clear() {
        contextHolder.remove()
    }
}
