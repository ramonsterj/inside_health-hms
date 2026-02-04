package com.insidehealthgt.hms.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

@Service
@Suppress("TooGenericExceptionCaught") // Intentional: fail gracefully per FR12 - show placeholder on any error
class ThumbnailService {

    private val logger = LoggerFactory.getLogger(ThumbnailService::class.java)

    /**
     * Generate a thumbnail for a document.
     *
     * @param filePath Absolute path to the original file
     * @param contentType MIME type of the file
     * @return Thumbnail image as PNG bytes, or null if generation failed
     */
    fun generateThumbnail(filePath: Path, contentType: String): ByteArray? = try {
        when {
            contentType == "application/pdf" -> generatePdfThumbnail(filePath)
            contentType.startsWith("image/") -> generateImageThumbnail(filePath)
            else -> null
        }
    } catch (ex: Exception) {
        logger.warn("Failed to generate thumbnail for {}: {}", filePath, ex.message)
        null
    }

    /**
     * Generate thumbnail for PDF by rendering the first page.
     */
    private fun generatePdfThumbnail(filePath: Path): ByteArray? {
        return try {
            Loader.loadPDF(filePath.toFile()).use { document ->
                if (document.numberOfPages == 0) {
                    return null
                }

                val renderer = PDFRenderer(document)
                // Render at 72 DPI for reasonable thumbnail quality
                val pageImage = renderer.renderImageWithDPI(0, DPI_FOR_THUMBNAIL)

                // Scale to thumbnail size
                val thumbnail = scaleImage(pageImage)

                // Convert to PNG bytes
                val outputStream = ByteArrayOutputStream()
                ImageIO.write(thumbnail, "png", outputStream)
                outputStream.toByteArray()
            }
        } catch (ex: Exception) {
            logger.warn("Failed to generate PDF thumbnail: {}", ex.message)
            null
        }
    }

    /**
     * Generate thumbnail for image by resizing.
     */
    private fun generateImageThumbnail(filePath: Path): ByteArray? {
        return try {
            val originalImage = Files.newInputStream(filePath).use { inputStream ->
                ImageIO.read(inputStream)
            } ?: return null

            val thumbnail = scaleImage(originalImage)

            val outputStream = ByteArrayOutputStream()
            ImageIO.write(thumbnail, "png", outputStream)
            outputStream.toByteArray()
        } catch (ex: Exception) {
            logger.warn("Failed to generate image thumbnail: {}", ex.message)
            null
        }
    }

    /**
     * Scale an image to fit within the thumbnail dimensions while preserving aspect ratio.
     */
    private fun scaleImage(original: BufferedImage): BufferedImage {
        val originalWidth = original.width
        val originalHeight = original.height

        // Calculate scale to fit within max dimensions
        val widthRatio = THUMBNAIL_MAX_WIDTH.toDouble() / originalWidth
        val heightRatio = THUMBNAIL_MAX_HEIGHT.toDouble() / originalHeight
        val scale = minOf(widthRatio, heightRatio, 1.0) // Don't upscale

        val newWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
        val newHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

        val thumbnail = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = thumbnail.createGraphics()
        graphics.drawImage(original, 0, 0, newWidth, newHeight, null)
        graphics.dispose()

        return thumbnail
    }

    companion object {
        const val THUMBNAIL_MAX_WIDTH = 200
        const val THUMBNAIL_MAX_HEIGHT = 200
        private const val DPI_FOR_THUMBNAIL = 72f
    }
}
