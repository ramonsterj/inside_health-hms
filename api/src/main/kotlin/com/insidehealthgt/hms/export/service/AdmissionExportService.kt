package com.insidehealthgt.hms.export.service

import com.insidehealthgt.hms.exception.PayloadTooLargeException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.export.dto.AdmissionExportSnapshot
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.service.MessageService
import com.insidehealthgt.hms.util.AdmissionExportDateFormatter
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Locale
import java.util.stream.Stream

/**
 * Container handed back to the controller. The controller owns streaming [pdfFile]
 * to the HTTP response and deleting [tempDirectory] in a finally block.
 */
data class GeneratedExport(
    val pdfFile: Path,
    val tempDirectory: Path,
    val filename: String,
    val sha256: String,
    val byteSize: Long,
)

/**
 * Orchestrates synchronous generation of the admission-export PDF.
 *
 * Lifecycle per `docs/features/admission-export.md` v1.2:
 *  1. Read snapshot inside REPEATABLE_READ transaction (no PHI leaks past).
 *  2. Pre-flight 500 MB size cap; failures yield a `FAILED` audit row + 413.
 *  3. Render body PDF + merge appendix into a request-scoped OS temp directory.
 *  4. Compute SHA-256 of finalized file, write `SUCCESS` audit row.
 *  5. Hand back [GeneratedExport]; controller streams + wipes temp dir.
 */
@Service
@Suppress("LongParameterList", "TooGenericExceptionCaught", "TooManyFunctions")
class AdmissionExportService(
    private val snapshotService: AdmissionExportSnapshotService,
    private val renderer: AdmissionExportRenderer,
    private val appendixBuilder: AdmissionExportAppendixBuilder,
    private val auditWriter: AdmissionExportAuditWriter,
    private val messageService: MessageService,
) {

    private val log = LoggerFactory.getLogger(AdmissionExportService::class.java)

    fun generate(admissionId: Long, currentUser: CustomUserDetails, request: HttpServletRequest?): GeneratedExport {
        val context = ExportContext(
            admissionId = admissionId,
            locale = resolveLocale(currentUser),
            generatedAt = LocalDateTime.now(),
            userId = currentUser.id,
            username = currentUser.username,
            ipAddress = request?.remoteAddr,
            generatedByName = displayName(currentUser),
        )

        val snapshot = fetchSnapshotOrAudit(context)
        enforceSizeCap(snapshot, context)
        return renderAndAudit(snapshot, context)
    }

    private fun fetchSnapshotOrAudit(context: ExportContext): AdmissionExportSnapshot = try {
        snapshotService.fetchSnapshot(
            admissionId = context.admissionId,
            generatedAt = context.generatedAt,
            generatedByUserId = context.userId,
            generatedByName = context.generatedByName,
        )
    } catch (ex: ResourceNotFoundException) {
        auditWriter.writeFailure(
            userId = context.userId,
            username = context.username,
            admissionId = context.admissionId,
            ipAddress = context.ipAddress,
            details = mapOf(
                "phase" to "snapshot",
                "errorClass" to ex.javaClass.simpleName,
                "generatedAt" to context.generatedAt.toString(),
            ),
        )
        throw ex
    }

    private fun enforceSizeCap(snapshot: AdmissionExportSnapshot, context: ExportContext) {
        val estimatedSize = estimateAppendixSize(snapshot)
        if (estimatedSize <= MAX_PAYLOAD_BYTES) return
        auditWriter.writeFailure(
            userId = context.userId,
            username = context.username,
            admissionId = context.admissionId,
            ipAddress = context.ipAddress,
            details = mapOf(
                "phase" to "preflight",
                "estimatedBytes" to estimatedSize,
                "limit" to MAX_PAYLOAD_BYTES,
                "generatedAt" to context.generatedAt.toString(),
            ),
        )
        throw PayloadTooLargeException(
            messageService.getMessage("admission.export.error.too_large", context.locale),
        )
    }

    private fun renderAndAudit(snapshot: AdmissionExportSnapshot, context: ExportContext): GeneratedExport {
        val tempDir = Files.createTempDirectory("admission-export-")
        var success = false
        try {
            val preparedAppendix = appendixBuilder.buildAppendix(snapshot.attachments, tempDir, context.locale)
            val bodyPdf = renderBodyWithConvergedIndex(snapshot, context.locale, preparedAppendix, tempDir)
            val finalPdf = mergeFinalPdf(bodyPdf, preparedAppendix.pdfFile, tempDir)

            val sha256 = computeSha256(finalPdf)
            val byteSize = Files.size(finalPdf)
            val filename = buildFilename(snapshot, context.generatedAt)

            auditWriter.writeSuccess(
                userId = context.userId,
                username = context.username,
                admissionId = context.admissionId,
                ipAddress = context.ipAddress,
                details = mapOf(
                    "generatedAt" to context.generatedAt.toString(),
                    "byteSize" to byteSize,
                    "sha256" to sha256,
                    "attachmentCount" to snapshot.attachments.size,
                    "skippedAttachmentIds" to preparedAppendix.skippedAttachmentIds,
                    "sectionCounts" to sectionCounts(snapshot),
                ),
            )

            success = true
            return GeneratedExport(
                pdfFile = finalPdf,
                tempDirectory = tempDir,
                filename = filename,
                sha256 = sha256,
                byteSize = byteSize,
            )
        } catch (ex: Exception) {
            log.error("Admission export failed for admission {}", context.admissionId, ex)
            auditWriter.writeFailure(
                userId = context.userId,
                username = context.username,
                admissionId = context.admissionId,
                ipAddress = context.ipAddress,
                details = mapOf(
                    "phase" to "render",
                    "errorClass" to ex.javaClass.simpleName,
                    "generatedAt" to context.generatedAt.toString(),
                ),
            )
            throw ex
        } finally {
            if (!success) {
                deleteRecursively(tempDir)
            }
        }
    }

    private fun renderBodyWithConvergedIndex(
        snapshot: AdmissionExportSnapshot,
        locale: Locale,
        preparedAppendix: AdmissionExportAppendixBuilder.PreparedAppendix,
        tempDir: Path,
    ): Path {
        val bodyPdf = tempDir.resolve("body.pdf")
        var bodyPageCount = 0
        var indexEntries = preparedAppendix.indexEntries
        var converged = false
        for (attempt in 0 until MAX_BODY_RENDER_PASSES) {
            Files.newOutputStream(bodyPdf).use { out ->
                renderer.render(snapshot, locale, out, indexEntries)
            }
            val newBodyPageCount = appendixBuilder.countPages(bodyPdf)
            val nextEntries = preparedAppendix.indexEntries.map { entry ->
                entry.copy(appendixPageNumber = newBodyPageCount + entry.relativeAppendixPageNumber)
            }
            if (newBodyPageCount == bodyPageCount && nextEntries == indexEntries) {
                converged = true
                break
            }
            bodyPageCount = newBodyPageCount
            indexEntries = nextEntries
        }
        if (!converged) {
            Files.newOutputStream(bodyPdf).use { out ->
                renderer.render(snapshot, locale, out, indexEntries)
            }
        }
        return bodyPdf
    }

    private fun mergeFinalPdf(bodyPdf: Path, appendixPdf: Path?, tempDir: Path): Path {
        val finalPdf = tempDir.resolve("final.pdf")
        Files.newOutputStream(finalPdf).use { out ->
            appendixBuilder.mergeBodyAndAppendix(bodyPdf, appendixPdf, out)
        }
        return finalPdf
    }

    private fun displayName(currentUser: CustomUserDetails): String = currentUser.getUser().let {
        listOfNotNull(it.firstName, it.lastName)
            .joinToString(" ")
            .ifBlank { it.username }
    }

    private data class ExportContext(
        val admissionId: Long,
        val locale: Locale,
        val generatedAt: LocalDateTime,
        val userId: Long?,
        val username: String?,
        val ipAddress: String?,
        val generatedByName: String,
    )

    private fun resolveLocale(currentUser: CustomUserDetails): Locale {
        val pref = currentUser.localePreference?.takeIf { it.isNotBlank() }
        if (pref != null) {
            return Locale.forLanguageTag(pref.replace('_', '-'))
        }
        return LocaleContextHolder.getLocale()
    }

    private fun estimateAppendixSize(snapshot: AdmissionExportSnapshot): Long {
        val attachmentBytes = snapshot.attachments.sumOf { it.byteSize }
        return attachmentBytes + BODY_SIZE_ESTIMATE
    }

    private fun sectionCounts(snapshot: AdmissionExportSnapshot): Map<String, Int> = mapOf(
        "progressNotes" to snapshot.progressNotes.size,
        "medicalOrders" to snapshot.medicalOrders.size,
        "nursingNotes" to snapshot.nursingNotes.size,
        "vitalSigns" to snapshot.vitalSigns.size,
        "medicationAdministrations" to snapshot.medicationAdministrations.size,
        "psychotherapyActivities" to snapshot.psychotherapyActivities.size,
        "patientCharges" to snapshot.patientCharges.size,
        "invoices" to snapshot.invoices.size,
        "consultingPhysicians" to snapshot.consultingPhysicians.size,
        "emergencyContacts" to snapshot.emergencyContacts.size,
        "attachments" to snapshot.attachments.size,
    )

    private fun buildFilename(snapshot: AdmissionExportSnapshot, generatedAt: LocalDateTime): String {
        val sanitizedLast = sanitizeForFilename(snapshot.patient.lastName)
        val timestamp = AdmissionExportDateFormatter.filenameTimestamp(generatedAt)
        return "admission-${snapshot.admission.id}-$sanitizedLast-$timestamp.pdf"
    }

    private fun sanitizeForFilename(value: String): String {
        val cleaned = value.replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-', '.')
        return cleaned.ifBlank { "patient" }.lowercase()
    }

    private fun computeSha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(SHA256_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val MAX_PAYLOAD_BYTES: Long = 500L * 1024 * 1024 // 500 MB
        private const val BODY_SIZE_ESTIMATE: Long = 5L * 1024 * 1024 // assume body never exceeds 5 MB
        private const val MAX_BODY_RENDER_PASSES = 3
        private const val SHA256_BUFFER = 8192

        fun deleteRecursively(path: Path) {
            if (!Files.exists(path)) return
            Files.walk(path).use { stream: Stream<Path> ->
                stream.sorted(Comparator.reverseOrder()).forEach { p ->
                    runCatching { Files.deleteIfExists(p) }
                }
            }
        }
    }
}
