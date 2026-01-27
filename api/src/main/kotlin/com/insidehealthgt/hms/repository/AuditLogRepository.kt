package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    fun findByUserId(userId: Long, pageable: Pageable): Page<AuditLog>

    fun findByEntityTypeAndEntityId(entityType: String, entityId: Long, pageable: Pageable): Page<AuditLog>

    fun findByAction(action: AuditAction, pageable: Pageable): Page<AuditLog>

    fun findByEntityType(entityType: String, pageable: Pageable): Page<AuditLog>

    @Query(
        """
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = :userId)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        ORDER BY a.timestamp DESC
    """,
    )
    fun findByFilters(
        @Param("userId") userId: Long?,
        @Param("entityType") entityType: String?,
        @Param("action") action: AuditAction?,
        pageable: Pageable,
    ): Page<AuditLog>

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a ORDER BY a.entityType")
    fun findDistinctEntityTypes(): List<String>
}
