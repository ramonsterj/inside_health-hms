package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    fun findByEntityTypeAndEntityId(entityType: String, entityId: Long, pageable: Pageable): Page<AuditLog>

    @Query(
        """
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = :userId)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        AND (:startDate IS NULL OR a.timestamp >= :startDate)
        AND (:endDate IS NULL OR a.timestamp <= :endDate)
        ORDER BY a.timestamp DESC
    """,
    )
    fun findByFilters(
        @Param("userId") userId: Long?,
        @Param("entityType") entityType: String?,
        @Param("action") action: AuditAction?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable,
    ): Page<AuditLog>

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a ORDER BY a.entityType")
    fun findDistinctEntityTypes(): List<String>

    @Query(
        "SELECT new com.insidehealthgt.hms.dto.response.AuditUserSummary(a.userId, a.username) " +
            "FROM AuditLog a WHERE a.userId IS NOT NULL GROUP BY a.userId, a.username ORDER BY a.username",
    )
    fun findDistinctUsers(): List<com.insidehealthgt.hms.dto.response.AuditUserSummary>
}
