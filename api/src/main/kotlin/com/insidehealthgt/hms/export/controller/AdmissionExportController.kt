package com.insidehealthgt.hms.export.controller

import com.insidehealthgt.hms.export.service.AdmissionExportService
import com.insidehealthgt.hms.security.CustomUserDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        response: HttpServletResponse,
    ) {
        val export = admissionExportService.generate(id, currentUser, request)
        try {
            // Stream the rendered PDF directly from disk to the servlet output
            // stream so the full payload never needs to be buffered in heap.
            // With the 500 MB pre-flight cap, buffering would let a single
            // export — or a few concurrent ones — exhaust the JVM heap.
            response.status = HttpServletResponse.SC_OK
            response.contentType = MediaType.APPLICATION_PDF_VALUE
            response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"${export.filename}\"",
            )
            response.setHeader("X-Admission-Export-Sha256", export.sha256)
            response.setContentLengthLong(export.byteSize)
            response.outputStream.use { out ->
                Files.copy(export.pdfFile, out)
                out.flush()
            }
        } finally {
            // Cleanup is synchronous (same thread, no async dispatch) so the
            // temp directory is gone before this handler returns. This is the
            // same invariant the previous buffered version held — we keep it
            // to avoid the StreamingResponseBody finally-block race that left
            // leftover `admission-export-*` directories in $TMPDIR.
            AdmissionExportService.deleteRecursively(export.tempDirectory)
        }

        log.info(
            "Admission export ready: admissionId={} bytes={} sha256={} filename={}",
            id,
            export.byteSize,
            export.sha256,
            export.filename,
        )
    }
}
