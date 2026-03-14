package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuditLogResponse
import com.insidehealthgt.hms.dto.response.AuditUserSummary
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.service.AuditLogService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAuthority('audit:read')")
class AuditLogController(private val auditLogService: AuditLogService) {

    @GetMapping
    fun getAuditLogs(
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?,
        @PageableDefault(size = 50, sort = ["timestamp"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> {
        val hasFilters = userId != null || entityType != null || action != null ||
            startDate != null || endDate != null
        val logs = if (hasFilters) {
            auditLogService.findByFilters(userId, entityType, action, startDate, endDate, pageable)
        } else {
            auditLogService.findAll(pageable)
        }
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(logs)))
    }

    @GetMapping("/entity")
    fun getAuditLogsForEntity(
        @RequestParam entityType: String,
        @RequestParam entityId: Long,
        @PageableDefault(size = 50, sort = ["timestamp"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> {
        val logs = auditLogService.findByEntity(entityType, entityId, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(logs)))
    }

    @GetMapping("/entity-types")
    fun getEntityTypes(): ResponseEntity<ApiResponse<List<String>>> {
        val entityTypes = auditLogService.getDistinctEntityTypes()
        return ResponseEntity.ok(ApiResponse.success(entityTypes))
    }

    @GetMapping("/users")
    fun getAuditUsers(): ResponseEntity<ApiResponse<List<AuditUserSummary>>> {
        val users = auditLogService.getDistinctUsers()
        return ResponseEntity.ok(ApiResponse.success(users))
    }
}
