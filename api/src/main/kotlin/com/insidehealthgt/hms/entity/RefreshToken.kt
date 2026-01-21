package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
@SQLRestriction("deleted_at IS NULL")
class RefreshToken(

    @Column(nullable = false, unique = true, length = 500)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

) : BaseEntity()
