package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "audit_logs")
class AuditLog(

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(length = 255)
    var username: String? = null,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var action: AuditAction,

    @Column(name = "entity_type", nullable = false, length = 255)
    var entityType: String,

    @Column(name = "entity_id", nullable = false)
    var entityId: Long,

    @Column(name = "old_values", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var oldValues: String? = null,

    @Column(name = "new_values", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var newValues: String? = null,

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "changed_fields", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var changedFields: String? = null,

    @Column(nullable = false)
    var timestamp: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
}
