package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyActivityRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PsychotherapyActivityResponse
import com.insidehealthgt.hms.service.PsychotherapyActivityService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/psychotherapy-activities")
class PsychotherapyActivityController(private val activityService: PsychotherapyActivityService) {

    /**
     * List activities for admission (all clinical staff can view).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('psychotherapy-activity:read')")
    fun listActivities(
        @PathVariable admissionId: Long,
        @RequestParam(defaultValue = "desc") sort: String,
    ): ResponseEntity<ApiResponse<List<PsychotherapyActivityResponse>>> {
        val activities = activityService.listActivities(admissionId, sort)
        return ResponseEntity.ok(ApiResponse.success(activities))
    }

    /**
     * Get single activity.
     */
    @GetMapping("/{activityId}")
    @PreAuthorize("hasAuthority('psychotherapy-activity:read')")
    fun getActivity(
        @PathVariable admissionId: Long,
        @PathVariable activityId: Long,
    ): ResponseEntity<ApiResponse<PsychotherapyActivityResponse>> {
        val activity = activityService.getActivity(admissionId, activityId)
        return ResponseEntity.ok(ApiResponse.success(activity))
    }

    /**
     * Create activity (psychologist only).
     * Service layer enforces PSYCHOLOGIST role check.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('psychotherapy-activity:create')")
    fun createActivity(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreatePsychotherapyActivityRequest,
    ): ResponseEntity<ApiResponse<PsychotherapyActivityResponse>> {
        val activity = activityService.createActivity(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(activity))
    }

    /**
     * Delete activity (admin only via permission).
     */
    @DeleteMapping("/{activityId}")
    @PreAuthorize("hasAuthority('psychotherapy-activity:delete')")
    fun deleteActivity(@PathVariable admissionId: Long, @PathVariable activityId: Long): ResponseEntity<Void> {
        activityService.deleteActivity(admissionId, activityId)
        return ResponseEntity.noContent().build()
    }
}
