package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateNursingNoteRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.NursingNoteResponse
import com.insidehealthgt.hms.service.NursingNoteService
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
@RequestMapping("/api/v1/admissions/{admissionId}/nursing-notes")
class NursingNoteController(private val nursingNoteService: NursingNoteService) {

    @GetMapping
    @PreAuthorize("hasAuthority('nursing-note:read')")
    fun listNursingNotes(
        @PathVariable admissionId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<NursingNoteResponse>>> {
        val notes = nursingNoteService.listNursingNotes(admissionId, pageable)
        return ResponseEntity.ok(ApiResponse.success(notes))
    }

    @GetMapping("/{noteId}")
    @PreAuthorize("hasAuthority('nursing-note:read')")
    fun getNursingNote(
        @PathVariable admissionId: Long,
        @PathVariable noteId: Long,
    ): ResponseEntity<ApiResponse<NursingNoteResponse>> {
        val note = nursingNoteService.getNursingNote(admissionId, noteId)
        return ResponseEntity.ok(ApiResponse.success(note))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('nursing-note:create')")
    fun createNursingNote(
        @PathVariable admissionId: Long,
        @Valid @RequestBody request: CreateNursingNoteRequest,
    ): ResponseEntity<ApiResponse<NursingNoteResponse>> {
        val note = nursingNoteService.createNursingNote(admissionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(note))
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAuthority('nursing-note:update')")
    fun updateNursingNote(
        @PathVariable admissionId: Long,
        @PathVariable noteId: Long,
        @Valid @RequestBody request: UpdateNursingNoteRequest,
    ): ResponseEntity<ApiResponse<NursingNoteResponse>> {
        val note = nursingNoteService.updateNursingNote(admissionId, noteId, request)
        return ResponseEntity.ok(ApiResponse.success(note))
    }
}
