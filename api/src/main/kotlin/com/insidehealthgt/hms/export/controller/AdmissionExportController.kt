package com.insidehealthgt.hms.export.controller

import com.insidehealthgt.hms.export.service.AdmissionExportService
import com.insidehealthgt.hms.security.CustomUserDetails
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files

@RestController
@RequestMapping("/api/v1/admissions")
class AdmissionExportController(private val admissionExportService: AdmissionExportService) {

    private val log = LoggerFactory.getLogger(AdmissionExportController::class.java)

    @GetMapping("/{id}/export.pdf")
    @PreAuthorize("hasAuthority('admission:export-pdf')")
    fun exportAdmission(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
        request: HttpServletRequest,
    ): ResponseEntity<StreamingResponseBody> {
        val export = admissionExportService.generate(id, currentUser, request)
        val streamingBody = StreamingResponseBody { outputStream ->
            try {
                Files.newInputStream(export.pdfFile).use { input ->
                    input.copyTo(outputStream)
                }
            } finally {
                AdmissionExportService.deleteRecursively(export.tempDirectory)
            }
        }

        log.info(
            "Admission export ready: admissionId={} bytes={} sha256={} filename={}",
            id,
            export.byteSize,
            export.sha256,
            export.filename,
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${export.filename}\"")
            .header("X-Admission-Export-Sha256", export.sha256)
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(export.byteSize)
            .body(streamingBody)
    }
}
