package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateProgressNoteRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.ProgressNoteResponse
import com.insidehealthgt.hms.service.ProgressNoteService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/progress-notes")
class ProgressNoteController(private val progressNoteService: ProgressNoteService) {

    @GetMapping
    @PreAuthorize("hasAuthority('progress-note:read')")
    fun listProgressNotes(
        @PathVariable admissionId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<ProgressNoteResponse>>> {
        val notes = progressNoteService.listProgressNotes(admissionId, pageable)
        return ResponseEntity.ok(ApiResponse.success(notes))
    }

    @GetMapping("/{noteId}")
    @PreAuthorize("hasAuthority('progress-note:read')")
    fun getProgressNote(
        @PathVariable admissionId: Long,
        @PathVariable noteId: Long,
    ): ResponseEntity<ApiResponse<ProgressNoteResponse>> {
        val note = progressNoteService.getProgressNote(admissionId, noteId)
        return ResponseEntity.ok(ApiResponse.success(note))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('progress-note:create')")
    fun createProgressNote(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateProgressNoteRequest,
    ): ResponseEntity<ApiResponse<ProgressNoteResponse>> {
        val note = progressNoteService.createProgressNote(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(note))
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAuthority('progress-note:update')")
    fun updateProgressNote(
        @PathVariable admissionId: Long,
        @PathVariable noteId: Long,
        @Valid @RequestBody request: UpdateProgressNoteRequest,
    ): ResponseEntity<ApiResponse<ProgressNoteResponse>> {
        val note = progressNoteService.updateProgressNote(admissionId, noteId, request)
        return ResponseEntity.ok(ApiResponse.success(note))
    }
}
