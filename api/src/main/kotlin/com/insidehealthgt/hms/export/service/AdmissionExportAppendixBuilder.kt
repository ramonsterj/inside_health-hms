package com.insidehealthgt.hms.export.service

import com.insidehealthgt.hms.export.dto.AttachmentIndexEntry
import com.insidehealthgt.hms.export.dto.AttachmentSnapshot
import com.insidehealthgt.hms.service.FileStorageService
import com.insidehealthgt.hms.service.MessageService
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale

/**
 * Merges the body PDF with the binary appendix (consent forms, ID documents,
 * admission attachments, and medical-order documents). Each attachment is preceded
 * by a one-page separator listing its filename, source, and SHA-256 checksum.
 * PDFs are merged page-by-page; PNG/JPG/JPEG attachments are placed centered on
 * a fitted page. Unsupported MIME types produce a separator page with a
 * "not embeddable" notice rather than smuggling raw bytes.
 */
@Service
@Suppress("TooGenericExceptionCaught", "TooManyFunctions", "LongMethod")
class AdmissionExportAppendixBuilder(
    private val fileStorageService: FileStorageService,
    private val messageService: MessageService,
) {

    private val log = LoggerFactory.getLogger(AdmissionExportAppendixBuilder::class.java)

    data class PreparedAppendix(
        val pdfFile: Path?,
        val indexEntries: List<AttachmentIndexEntry>,
        val skippedAttachmentIds: List<Long>,
    )

    private data class RenderedAttachment(val pdfFile: Path, val checksum: String)

    fun buildAppendix(attachments: List<AttachmentSnapshot>, tempDir: Path, locale: Locale): PreparedAppendix {
        if (attachments.isEmpty()) {
            return PreparedAppendix(pdfFile = null, indexEntries = emptyList(), skippedAttachmentIds = emptyList())
        }
        val sources = mutableListOf<Path>()
        val indexEntries = mutableListOf<AttachmentIndexEntry>()
        val skipped = mutableListOf<Long>()
        var nextAppendixPage = 1

        attachments.forEachIndexed { index, attachment ->
            try {
                val attachmentPdf = renderAttachment(attachment, tempDir, index, locale)
                sources.add(attachmentPdf.pdfFile)
                indexEntries += AttachmentIndexEntry(
                    attachmentId = attachment.id,
                    source = attachment.source,
                    checksum = attachmentPdf.checksum,
                    relativeAppendixPageNumber = nextAppendixPage,
                )
                nextAppendixPage += countPages(attachmentPdf.pdfFile)
            } catch (ex: Exception) {
                log.warn("Failed to embed attachment {} ({}): {}", attachment.id, attachment.source, ex.message)
                skipped += attachment.id
                runCatching {
                    val fallback = writeFailureSeparator(attachment, tempDir, index, locale)
                    sources.add(fallback)
                    indexEntries += AttachmentIndexEntry(
                        attachmentId = attachment.id,
                        source = attachment.source,
                        checksum = "",
                        relativeAppendixPageNumber = nextAppendixPage,
                        skipped = true,
                    )
                    nextAppendixPage += countPages(fallback)
                }
            }
        }

        val appendixPdf = tempDir.resolve("appendix.pdf")
        val merger = PDFMergerUtility()
        merger.destinationFileName = appendixPdf.toString()
        sources.forEach { merger.addSource(it.toFile()) }
        merger.mergeDocuments(null)
        return PreparedAppendix(
            pdfFile = appendixPdf,
            indexEntries = indexEntries,
            skippedAttachmentIds = skipped,
        )
    }

    fun mergeBodyAndAppendix(bodyPdf: Path, appendixPdf: Path?, output: OutputStream) {
        val merger = PDFMergerUtility()
        merger.destinationStream = output
        merger.addSource(bodyPdf.toFile())
        appendixPdf?.let { merger.addSource(it.toFile()) }
        merger.mergeDocuments(null)
    }

    fun countPages(path: Path): Int = Loader.loadPDF(path.toFile()).use { it.numberOfPages }

    private fun renderAttachment(
        attachment: AttachmentSnapshot,
        tempDir: Path,
        index: Int,
        locale: Locale,
    ): RenderedAttachment {
        val bytes = fileStorageService.loadFile(attachment.storagePath)
        val sha256 = sha256(bytes)
        val separator = writeSeparator(attachment, sha256, tempDir, index, locale, embedded = true)

        val mimeType = attachment.contentType.lowercase()
        val pdf = when {
            mimeType == "application/pdf" -> mergeSeparatorAndPdf(separator, bytes, tempDir, index)
            mimeType in IMAGE_MIME_TYPES -> mergeSeparatorAndImage(separator, bytes, tempDir, index)
            else -> writeSeparator(attachment, sha256, tempDir, index, locale, embedded = false)
        }
        return RenderedAttachment(pdfFile = pdf, checksum = sha256)
    }

    private fun mergeSeparatorAndPdf(separator: Path, pdfBytes: ByteArray, tempDir: Path, index: Int): Path {
        val attachmentPdfPath = tempDir.resolve("attachment-$index.pdf")
        Files.write(attachmentPdfPath, pdfBytes)
        val combined = tempDir.resolve("attachment-$index-combined.pdf")
        val merger = PDFMergerUtility()
        merger.destinationFileName = combined.toString()
        merger.addSource(separator.toFile())
        merger.addSource(attachmentPdfPath.toFile())
        merger.mergeDocuments(null)
        return combined
    }

    private fun mergeSeparatorAndImage(separator: Path, imageBytes: ByteArray, tempDir: Path, index: Int): Path {
        val imagePdf = tempDir.resolve("attachment-$index-image.pdf")
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val image = PDImageXObject.createFromByteArray(doc, imageBytes, "attachment-$index")
            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height
            val ratio = minOf(pageWidth / image.width, pageHeight / image.height)
            val drawWidth = image.width * ratio
            val drawHeight = image.height * ratio
            val x = (pageWidth - drawWidth) / 2f
            val y = (pageHeight - drawHeight) / 2f
            PDPageContentStream(doc, page).use { content ->
                content.drawImage(image, x, y, drawWidth, drawHeight)
            }
            doc.save(imagePdf.toFile())
        }
        val combined = tempDir.resolve("attachment-$index-combined.pdf")
        val merger = PDFMergerUtility()
        merger.destinationFileName = combined.toString()
        merger.addSource(separator.toFile())
        merger.addSource(imagePdf.toFile())
        merger.mergeDocuments(null)
        return combined
    }

    private fun writeSeparator(
        attachment: AttachmentSnapshot,
        sha256: String,
        tempDir: Path,
        index: Int,
        locale: Locale,
        embedded: Boolean,
    ): Path = renderSeparatorPdf(
        attachment = attachment,
        sha256 = sha256,
        noticeKey = if (embedded) {
            "admission.export.appendix.embedded_notice"
        } else {
            "admission.export.appendix.not_embeddable_notice"
        },
        outputPath = tempDir.resolve("separator-$index.pdf"),
        locale = locale,
    )

    private fun writeFailureSeparator(
        attachment: AttachmentSnapshot,
        tempDir: Path,
        index: Int,
        locale: Locale,
    ): Path = renderSeparatorPdf(
        attachment = attachment,
        sha256 = "",
        noticeKey = "admission.export.appendix.failure_notice",
        outputPath = tempDir.resolve("separator-failure-$index.pdf"),
        locale = locale,
    )

    private fun renderSeparatorPdf(
        attachment: AttachmentSnapshot,
        sha256: String,
        noticeKey: String,
        outputPath: Path,
        locale: Locale,
    ): Path {
        val notice = messageService.getMessage(noticeKey, locale)
        val html = separatorHtml(attachment, sha256, notice, locale)
        Files.newOutputStream(outputPath).use { out ->
            val builder = PdfRendererBuilder()
            builder.withHtmlContent(html, null)
            builder.toStream(out)
            builder.run()
        }
        return outputPath
    }

    private fun separatorHtml(attachment: AttachmentSnapshot, sha256: String, notice: String, locale: Locale): String {
        val title = messageService.getMessage("admission.export.appendix.title", locale)
        val source = messageService.getMessage(
            "admission.export.attachmentSource.${attachment.source.name.lowercase()}",
            locale,
        )
        val labelFile = messageService.getMessage("admission.export.field.fileName", locale)
        val labelSource = messageService.getMessage("admission.export.field.source", locale)
        val labelChecksum = messageService.getMessage("admission.export.field.checksum", locale)
        val labelMime = messageService.getMessage("admission.export.field.contentType", locale)
        val labelSize = messageService.getMessage("admission.export.field.fileSize", locale)
        return """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"/>
            <style>
              @page { size: A4; margin: 18mm; }
              body { font-family: 'Helvetica', sans-serif; font-size: 11pt; color: #222; }
              h1 { font-size: 16pt; margin-bottom: 8pt; }
              .notice { background: #f3f3f3; padding: 8pt; border-left: 4pt solid #444; margin: 12pt 0; }
              table { width: 100%; border-collapse: collapse; }
              td { padding: 4pt 6pt; vertical-align: top; border: 1px solid #aaa; }
              .label { background: #eee; width: 30%; font-weight: bold; }
            </style></head>
            <body>
              <h1>${AdmissionExportRenderer.escape(title)}</h1>
              <table>
                <tr><td class="label">${AdmissionExportRenderer.escape(labelFile)}</td>
                    <td>${AdmissionExportRenderer.escape(attachment.fileName)}</td></tr>
                <tr><td class="label">${AdmissionExportRenderer.escape(labelSource)}</td>
                    <td>${AdmissionExportRenderer.escape(source)}</td></tr>
                <tr><td class="label">${AdmissionExportRenderer.escape(labelMime)}</td>
                    <td>${AdmissionExportRenderer.escape(attachment.contentType)}</td></tr>
                <tr><td class="label">${AdmissionExportRenderer.escape(labelSize)}</td>
                    <td>${attachment.byteSize}</td></tr>
                <tr><td class="label">${AdmissionExportRenderer.escape(labelChecksum)}</td>
                    <td>${AdmissionExportRenderer.escape(sha256)}</td></tr>
              </table>
              <div class="notice">${AdmissionExportRenderer.escape(notice)}</div>
            </body></html>
        """.trimIndent()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val IMAGE_MIME_TYPES = setOf("image/png", "image/jpeg", "image/jpg")
    }
}
